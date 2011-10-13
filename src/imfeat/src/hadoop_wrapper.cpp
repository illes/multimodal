#include <iostream>
#include <stdint.h>
#include <string>
#include <sstream>
#include <libgen.h>

#include "hadoop/Pipes.hh"
#include "hadoop/TemplateFactory.hh"
#include "hadoop/StringUtils.hh"
#include "hadoop/SerialUtils.hh"

#include "HDFSFile.h"
#include "histogram.h"

namespace HadoopUtils {
	/** 
	* A class to write a stream to a string.
	*/
	class StringOutStream: public OutStream {
		private:
			std::stringstream mStream;
		public:

			/**
			 * Create a stream.
			 */
			StringOutStream() : mStream(std::stringstream::out) { }

			void write(const void* buf, size_t len) { mStream.write((char*)buf, len); };
			void flush() { mStream.flush(); };
		
			/**
			 * Returns a copy of the string object currently associated with the string stream buffer.
			 */
			std::string str() const { return mStream.str(); }

			/**
			 * Returns a pointer to the stringbuf object associated with the stream.
			 */
			std::stringbuf* rdbuf ( ) const { return mStream.rdbuf(); }
	
			virtual ~StringOutStream() {};
	};

};

class ImgProcMap: public HadoopPipes::Mapper {
	public:
		enum ImgAlgo {
			SIFT = 0,
			HISTOGRAM
		};
	public:
		ImgProcMap(HadoopPipes::TaskContext& context) {
			const HadoopPipes::JobConf *conf = context.getJobConf ();
			HADOOP_ASSERT (conf != NULL, "There's no JobConf!");

			bool hasAlgo = conf->hasKey ("multimodal.img.algo");
			HADOOP_ASSERT (hasAlgo != false, "No image processing algorithm defined in conf file!");

			algo = context.getJobConf ()->getInt ("multimodal.img.algo");
		}

		void map(HadoopPipes::MapContext& context) {
			std::string k = context.getInputKey ();
			std::string v = context.getInputValue ();
			std::vector<char> img (v.begin (), v.end ());

			switch (algo) {
				case SIFT:
				{
					/* do sift */
					break;
				}

				case HISTOGRAM:
				{
					Histogram h (4, 4, 3);
					std::vector<double> hv;
					h.getHSV (img, hv);

					// serialize to Java's VectorWritable
					HadoopUtils::StringOutStream buf;
					serializeDoubleVector(hv, buf);
					context.emit (k, buf.str());

					break;
				}
				default:
					/* should never ever get here! */
					std::cerr << "undefined algorithm" << std::endl;
			}
		}

	private:
	int algo; /* image processing algorithm to run */
	
  	static const int8_t FLAG_DENSE = 0x01;
        static const int8_t FLAG_SEQUENTIAL = 0x02;
        static const int8_t FLAG_NAMED = 0x04;
        static const int8_t FLAG_LAX_PRECISION = 0x08; // set: double, unset: float
	static const int NUM_FLAGS = 4;

	/**
	 * Serialize into bytes readable as a VectorWritable.
	 *
	 * @see org.apache.mahout.math.VectorWritable.writeVector(DataOutput, Vector, boolean)
	 */
	static void serializeFloatVector(std::vector<float> a, HadoopUtils::OutStream& stream)
	{
		int8_t flags = FLAG_DENSE;
		stream.write(&flags, 1);
		writeUnsignedVarInt(a.size(), stream);
		for ( std::vector<float>::const_iterator it = a.begin(); it != a.end(); ++it) {
			stream.write(&(*it), 4);
		}

		return;
	}

	/**
	 * Serialize into bytes readable as a VectorWritable.
	 *
	 * @see org.apache.mahout.math.VectorWritable.writeVector(DataOutput, Vector, boolean)
	 */
	static void serializeDoubleVector(std::vector<double> a, HadoopUtils::OutStream& stream)
	{
		int8_t flags = FLAG_DENSE | FLAG_LAX_PRECISION;
		stream.write(&flags, 1);
		writeUnsignedVarInt(a.size(), stream);
		for ( std::vector<double>::const_iterator it = a.begin(); it != a.end(); ++it) {
			stream.write(&(*it), 8);
		}

		return;
	}

	static void writeUnsignedVarInt(int value, HadoopUtils::OutStream& out) {
		int8_t buf;
		while ((value & 0xFFFFFF80) != 0) {
		        buf = ((value & 0x7F) | 0x80);
			out.write(&buf, 1);
		        value >>= 7;
		}
		buf = (value & 0x7F);
		out.write(&buf, 1);
	}
};


class ImgProcReduce: public HadoopPipes::Reducer {
	public:
		ImgProcReduce(HadoopPipes::TaskContext& context){}
		void reduce(HadoopPipes::ReduceContext& context) {
			while (context.nextValue ()) {
//				std::cout << context.getInputKey () << std::endl;
				context.emit (context.getInputKey(), context.getInputValue ());
			}
		}
};

/* let's deserialize aa TextArrayWritable */
static void deserializeTextArrayWritable (const std::string& data, std::vector<std::string>& tv) {
	HadoopUtils::StringInStream stream (data);
	/* For some reason deserializeInt does not work, so going with our own implementation */
//	int32_t size = HadoopUtils::deserializeInt (stream);
	char b[4];
	for (int i = 0; i < 4; ++i) {
		stream.read (&b[i], 1);
	}
	int32_t size = ((b[0] << 24) | (b[1] << 16) | (b[2] << 8) | b[3]);
	int i = 0;
	while (i++ < size) {
		std::string fname;
		HadoopUtils::deserializeString (fname, stream);
		tv.push_back (fname);
	}

}

class ImgReader: public HadoopPipes::RecordReader {
	private:
		std::vector<std::string> fList;
		std::vector<char> buf;
		std::vector<std::string>::const_iterator it;
	public:
		ImgReader (HadoopPipes::MapContext& context) {
			std::string txtArray = context.getInputSplit();
			deserializeTextArrayWritable (txtArray, fList);
			it = fList.begin ();
		}

		virtual ~ImgReader () {
			buf.clear ();

		}

		virtual bool next (std::string& key, std::string& value) {
			/* currently transferring the whole image..
			 * TODO: support for splitting images with ROI
			 */
			if (it != fList.end ()) {
				std::string fname = *it;

				/* open file from HDFS */
				HDFSFile f (fname);
				if (!f.openRead () ) {
					std::cerr << "could not open file: " << key << std::endl;
				}
				/* read it into the buffer */
				f.read (buf);

				/* pass it as value */
				if (value.capacity () < buf.size ()) 
					value.resize (buf.size ());
				std::string v (buf.begin (), buf.end ());
				value = v;

				/* set key as the base filename without the path and extension */
				key = getBaseFilename (fname); 

				/* clear the buffer */
				buf.clear ();

				/* get next one */
				++it;

				return true;
			}

			return false;
		}

		virtual float getProgress () {
			return 1.0f;
		}
	private:

		std::string getBaseFilename (const std::string& fname) const {
			/* remove path */
			char * b = basename (const_cast<char*> (fname.c_str ()));
			std::string baseName (const_cast<const char*> (b));

			/* remove extension */
			size_t extPos = baseName.find_last_of ('.');

			return baseName.substr (0, extPos);
		}
};

class ImgWriter: public HadoopPipes::RecordWriter {
	public:
		ImgWriter (HadoopPipes::ReduceContext& context) {
		}

		virtual ~ImgWriter () {
		}

		virtual void emit(const std::string& key, const std::string& value) {
			std::cout << "been called with: " << key << std::endl;
		}
};	

int main (int argc, char **argv) {
	return HadoopPipes::runTask (HadoopPipes::TemplateFactory<ImgProcMap, ImgProcReduce, void, void, ImgReader>());
}

