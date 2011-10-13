package bme.iclef.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.MultipleOutputFormat;
import org.apache.hadoop.util.Progressable;

@SuppressWarnings("deprecation")
public class ImageOutputWriter<K,V> extends MultipleOutputFormat<K,V> {

	static protected class ImageRecordWriter<K,V> implements RecordWriter<K,V> {

		public ImageRecordWriter () {

		}

		public synchronized void write(K key, V value) throws IOException {
			boolean nullKey = key == null || key instanceof NullWritable;
			boolean nullValue = value == null || value instanceof NullWritable;

			if (nullKey && nullValue) {
				return;
			}

			System.out.println ("been called with key: " + key.getClass ());

		}

		public synchronized void close(Reporter reporter) throws IOException {

		}
	}


	@Override
	public RecordWriter<K,V> getBaseRecordWriter (FileSystem fs,
						  JobConf job,
						  String name,
						  Progressable arg3) throws IOException
	{

		return new ImageRecordWriter<K,V> ();
	
	}
};

