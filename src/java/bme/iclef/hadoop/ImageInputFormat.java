package bme.iclef.hadoop;

import java.io.*;
import java.util.*;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

public class ImageInputFormat extends FileInputFormat<IntWritable, Text> {

	public static final String FILES_PER_MAP = 
		    "mapreduce.input.imageinputformat.filespermap";


	static class TextArrayWritable extends ArrayWritable {
		public TextArrayWritable() {
			super(Text.class);
		}

		public String toString() {
			Writable[] w = get();
			StringBuffer buffer = new StringBuffer();
			String space = " ";

			for(int i = 0; i< w.length; i++) {
				buffer.append(w[i].toString()); 
				buffer.append(space);
			}
			return buffer.toString();
		}
	}

	static class ImageInputSplit implements InputSplit  {
		private TextArrayWritable list;

		ImageInputSplit() {}

		ImageInputSplit(Text[] uris) {
			this.list = new TextArrayWritable ();
			this.list.set (uris);
	       	}

		public void write(DataOutput out) throws IOException { 
			list.write (out);
		}

		public void readFields(DataInput in) throws IOException { 
			list = new TextArrayWritable ();
			list.readFields (in);
		}

		public long getLength() { return 0L; }
		public String[] getLocations() { return new String[0]; }
	}

	public InputSplit[] getSplits(JobConf conf, 
				      int numSplits) throws IOException {
		ArrayList<InputSplit> result = new ArrayList<InputSplit>();
		/* get the number of files per splits */
		long numFilesPerSplit = getNumFilesPerSplit (conf);
		FileSystem fs = FileSystem.get(conf);

		/* if it's 0 it means that we want to split whole process 
		 * into that many tasks as many map task capacity is there 
		 * available 
		 */
		if (numFilesPerSplit == 0) {
			long maxMapTasks = new JobClient (conf).getClusterStatus().getMaxMapTasks ();
			long numFiles = 0;
			for (Path dir: getInputPaths(conf)) {
				numFiles += fs.listStatus (dir).length;
			}

			numFilesPerSplit = numFiles/maxMapTasks;
		}

		for(Path dir: getInputPaths(conf)) {
			List<Text> split = new ArrayList<Text> ();
			long numFiles = 0;
			for (FileStatus f: fs.listStatus (dir)) {
				split.add (new Text (f.getPath ().toUri().getPath()));
				numFiles ++;

				if (numFiles == numFilesPerSplit) {
					ImageInputSplit imgSplit = 
						new ImageInputSplit (split.toArray (new Text[split.size ()]));
					result.add (imgSplit);
					numFiles = 0;
					split.clear ();
				}
			}
		}
		return result.toArray(new InputSplit[result.size()]);
	}

	public void validateInput(JobConf conf) { }

	public static long getNumFilesPerSplit(JobConf conf) {
		return conf.getInt(FILES_PER_MAP, 1);
	}

	public RecordReader<IntWritable, Text> getRecordReader(InputSplit split,
							       JobConf conf, 
							       Reporter reporter) {
		return new RecordReader<IntWritable, Text>(){
			public boolean next(IntWritable key, Text value) throws IOException {
				return false;
			}
			public IntWritable createKey() {
				return new IntWritable();
			}
			public Text createValue() {
				return new Text();
			}
			public long getPos() {
				return 0;
			}
			public void close() { }
			public float getProgress() { 
				return 0.0f;
			}
		};
	}

}

