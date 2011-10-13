package bme.iclef.hadoop;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataInput;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.MultipleOutputFormat;
import org.apache.hadoop.util.Progressable;

import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.utils.vectors.io.SequenceFileVectorWriter;

@SuppressWarnings("deprecation")
public class ImageOutputWriter<K,V> extends MultipleOutputFormat<K,V> {

	static final char keyIndexSeparator = ':'; 

	static protected class ImageRecordWriter<K,V> implements RecordWriter<K,V> {

		private SequenceFile.Writer out;

		public ImageRecordWriter (SequenceFile.Writer writer) {
			out = writer;
		}

		public synchronized void write(K key, V value) throws IOException {
			boolean nullKey = key == null || key instanceof NullWritable;
			boolean nullValue = value == null || value instanceof NullWritable;

			if (nullKey && nullValue) {
				return;
			}

			/* deserialize value */
		        DataInputStream in = new DataInputStream (new ByteArrayInputStream (.getBytes()));	
			Vector v = VectorWritable.readVector (in);

			/* TODO: it's possible to do the appending without deserialization
			 * SequenceFile.Writer API supports it... */
			out.append (key, v);

		}

		public synchronized void close(Reporter reporter) throws IOException {
			out.close ();
		}
	}

	@Override
	protected String generateFileNameForKeyValue (K key, V value, String name) {
		/* use the key as filename */
		if (key.getClass () == Text.class) {
			Text k = (Text) key;
			String fName = k.toString ();

		        /* handle multiple keys for one file */ 	
			int sepPos = -1;
			if ((sepPos = fName.lastIndexOf (keyIndexSeparator)) != -1) {
				/* remove key index suffix */
				fName = fName.substring (0, sepPos);
			}

			return fName;
		} 


		return super.generateFileNameForKeyValue (key, value, name);
	}

	@Override
	public RecordWriter<K,V> getBaseRecordWriter (FileSystem fs,
						  JobConf job,
						  String name,
						  Progressable arg3) throws IOException
	{

		Path fileOut = MultipleOutputFormat.getTaskOutputPath (job, name);
		FileSystem fileSys = fileOut.getFileSystem(job);
		SequenceFile.Writer writer = SequenceFile.createWriter (fileSys, job, fileOut, LongWritable.class, VectorWritable.class);

		return new ImageRecordWriter<K,V> (writer);
	
	}
};

