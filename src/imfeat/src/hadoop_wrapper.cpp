#include <arpa/inet.h> // htons()

#include <iostream>
#include <cstring> // memcpy
#include <stdint.h>
#include <string>
#include <sstream>
#include <libgen.h>
#include <vector>
#include <stdexcept>

#include "hadoop/Pipes.hh"
#include "hadoop/TemplateFactory.hh"
#include "hadoop/StringUtils.hh"
#include "hadoop/SerialUtils.hh"

#include "histogram.h"
#include "vldsift.hpp"
#include "FeatureExtractor.h"

static void deserializeBytes (std::vector<char> &b, HadoopUtils::InStream &stream);

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
			HISTOGRAM,
			DAISY
		};
		
		enum KeyPointDetector {
			MSER = 0,
			SIFT,
			SURF,
			ORB,
			YAPE
		};
		
	public:
		ImgProcMap(HadoopPipes::TaskContext& context) {
			const HadoopPipes::JobConf *conf = context.getJobConf ();
			HADOOP_ASSERT (conf != NULL, "There's no JobConf!");

			bool hasAlgo = conf->hasKey ("multimodal.img.algo");
			HADOOP_ASSERT (hasAlgo != false, "No image processing algorithm defined in conf file!");

			algo = context.getJobConf ()->getInt ("multimodal.img.algo");
			
			ft = NULL;
			
			switch (algo) {
				case SIFT:
				{
					ft = new VLDSIFT ();
					
					break;
				}
				
				case DAISY:
				{
					ft = new Daisy ();
					break;
				}
				
				case HISTOGRAM:
				{
					int hbin = 4, sbin = 4, vbin = 3;
					if (conf->hasKey ("multimodal.img.hist.hbin"))
						hbin = context.getJobConf ()->getInt ("multimodal.img.hist.hbin");
					if (conf->hasKey ("multimodal.img.hist.sbin"))
						sbin = context.getJobConf ()->getInt ("multimodal.img.hist.sbin");
					if (conf->hasKey ("multimodal.img.hist.vbin"))
						vbin = context.getJobConf ()->getInt ("multimodal.img.hist.vbin");
					
					ft = new Histogram (hbin, sbin, vbin);
					
					break;
				}
				
				default:
					/* should never ever get here! */
					std::cerr << "undefined algorithm" << std::endl;
			}
			
			HADOOP_ASSERT (ft != NULL, "Problem occured while constr img algo obj.");	
			
			if (conf->hasKey ("multimodal.img.keypoint"))
				setKeypointDetector (context.getJobConf ()->getInt ("multimodal.img.keypoint"));
		}

		~ImgProcMap () {
			if (ft)
				delete ft;
		}
		
		void map(HadoopPipes::MapContext& context) {
			/* get the base name of the file as key for the <K,V> pair */
			std::string k = getBaseFilename (context.getInputKey ());

			/* value is a serialized BytesArray so needs deserialization */
			std::string v = context.getInputValue ();
			//HadoopUtils::StringInStream stream (v);
			//std::vector<char> img;
			//deserializeBytes (img, stream);
			std::vector<char> img(v.begin(), v.end());

			/* debug info for deserialization problems:
			std::cout << "img size: " << img.size () << " value length: " << v.length () << std::endl;
			for (int i = 0 ; i < 5; ++i) {
				std::cout << (int)img[i] << " ";
			}
			std::cout << std::endl;
			*/

			/* generate features for the image */
			std::vector<std::vector<double> > descr;
			ft->getFeatures (img, descr);
			std::vector<std::vector<double> >::const_iterator it 
				= descr.begin (), it_end = descr.end ();
			for (int i = 0; it != it_end; it++, i++) {
				std::string key = k;
				/* append an index of the key to the filename
				 * if there are more than one vectors
				 */
				if (descr.size () > 1) {
					char idx[10];
					snprintf (idx, 9, ":%d",i); 
					key.append (idx);
				}
				/* serialize the vector and send it to the reducer */
				HadoopUtils::StringOutStream buf;
				serializeFloatVector(*it, buf);
				context.emit (key, buf.str ());
			}	
		}

	public:
		static void test() {
			std::vector<double> v = std::vector<double>();
			v.push_back(1.0);
			v.push_back(-1.0);
			HadoopUtils::StringOutStream buf;
	    		serializeFloatVector(v, buf, "hello");
			std::cout << buf.str();
		}


	private:
	int algo; /* image processing algorithm to run */
	FeatureExtractor *fe;
	
  	static const int8_t FLAG_DENSE = 0x01;
        static const int8_t FLAG_SEQUENTIAL = 0x02;
        static const int8_t FLAG_NAMED = 0x04;
        static const int8_t FLAG_LAX_PRECISION = 0x08; // unset: double, set: float
	static const int NUM_FLAGS = 4;

	/**
	 * Serialize into bytes readable as a VectorWritable. Yields (un)named DenseVector.
	 *
	 * @see org.apache.mahout.math.VectorWritable.writeVector(DataOutput, Vector, boolean)
	 */
	template<class T> static void serializeFloatVector(const std::vector<T> a, HadoopUtils::OutStream& stream, const char* const name = NULL, int namelen = -1)
	{
		uint8_t flags = FLAG_DENSE | FLAG_SEQUENTIAL | FLAG_LAX_PRECISION;
		if (name != NULL)
			flags |= FLAG_NAMED;
		stream.write(&flags, 1);
		writeUnsignedVarInt(a.size(), stream);
		for (typename std::vector<T>::const_iterator it = a.begin(); it != a.end(); ++it) {
			float f = (float) (*it);
			serializeFloat(f, stream);
		}
		if (name != NULL)
			serializeUTF(name, stream, namelen);
		return;
	}

	static void serializeFloat(float f, HadoopUtils::OutStream& stream) {
		uint32_t buf; 
		memcpy(&buf, &f, 4);
		buf = htonl(buf);
		stream.write(&buf, 4);
	}

	static void serializeShort(int16_t n, HadoopUtils::OutStream& stream) {
		n = htons(n);
		stream.write(&n, 2);
	}

	/**
	 * Same as java.io.DataOutput#writeUTF().
	 *
	 * See: http://download.oracle.com/javase/6/docs/api/java/io/DataInput.html#modified-utf-8
	 */
	static void serializeUTF(const char* name, HadoopUtils::OutStream& stream, int namelen = -1) {
		if (namelen == -1)
			namelen = strlen(name);
		if (namelen > 65535)
			throw std::overflow_error("Name length overflow");
		// look for character codes over 127 (0x7F)
		for (int i = 0; i < namelen; i++)
		{
			if (name[i] & 0x80)
				throw std::overflow_error("Name character code overflow (non-ASCII-7 not implemented yet)");
			if (name[i] == '\0')
				throw std::underflow_error("Name character code underflow (zero '\\0' not supported yet)");
		}
		// write length
		serializeShort(namelen, stream);
		// write caharcters
		stream.write(name, namelen);
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


	std::string getBaseFilename (const std::string& fname) const {
		/* remove path */
		char * b = basename (const_cast<char*> (fname.c_str ()));
		std::string baseName (const_cast<const char*> (b));

		/* remove extension */
		size_t extPos = baseName.find_last_of ('.');
		if (extPos >= 0)
			return baseName.substr (0, extPos);
		else
			return baseName;
	}
	
	void setKeypointDetector (int kpDetector) const {
		Ptr<FeatureDetector> kpDet = NULL;
		switch (kpDetector) {
			case MSER:
				kpDet = FeatureDetector::create ("MSER");
				break;
			case SIFT:
				kpDet = FeatureDetector::create ("SIFT");
				break;
			case SURF:
				kpDet = FeatureDetector::create ("SURF");
				break;
			case ORB:
				kpDet = FeatureDetector::create ("ORB");
				break;
			case YAPE:
				break;
			default:
				return false;
		}
		
		fe->setKeypointDetector (kpDet);
		
		return true;
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

/* For some reason deserializeInt does not work, so going with our own implementation */
static int32_t myDeserializeInt (HadoopUtils::InStream &stream) {
	char b[4];
	for (int i = 0; i < 4; ++i) {
		stream.read (&b[i], 1);
	}
	int32_t ret = (((int32_t)(b[0]) << 24) | ((int32_t)(b[1]) << 16) | ((int32_t)(b[2]) << 8) | (int32_t)(b[3]));

	std::cerr << "read integer: " << ret << std::endl;
	return ret;
}

/* let's deserialize aa TextArrayWritable */
static void deserializeTextArrayWritable (const std::string& data, std::vector<std::string>& tv) {
	HadoopUtils::StringInStream stream (data);
	/* For some reason deserializeInt does not work, so going with our own implementation */
//	int32_t size = HadoopUtils::deserializeInt (stream);
	int32_t size = myDeserializeInt(stream);
	int i = 0;
	while (i++ < size) {
		std::string fname;
		HadoopUtils::deserializeString (fname, stream);
		tv.push_back (fname);
	}
}

static void deserializeBytes (std::vector<char> &b, HadoopUtils::InStream &stream) {
	int32_t len = myDeserializeInt(stream);
    	if (len > 0) {
		// resize the array to the right length
		b.resize(len);
		/*
		// read into the array in 64k chunks 
		const int bufSize = 65536;
		int offset = 0;
		while (len > 0) {
			const int chunkLength = len > bufSize ? bufSize : len;
			stream.read(&(b[offset]), chunkLength);
			offset += chunkLength;
			len -= chunkLength;
		}*/
	  	stream.read(&(b[0]), len);
	} else {
		b.clear();
	} 
}

int main (int argc, char **argv) {
	if (argc == 2 && strcmp(argv[1], "test") == 0)
		ImgProcMap::test();
	else
		return HadoopPipes::runTask (HadoopPipes::TemplateFactory<ImgProcMap, ImgProcReduce>());
}

