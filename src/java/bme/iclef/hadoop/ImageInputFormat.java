package bme.iclef.hadoop;

import java.io.*;
import java.util.*;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

public class ImageInputFormat extends FileInputFormat<IntWritable, Text> {

	static class ImageInputSplit implements InputSplit  {
		private String filename;

		ImageInputSplit() { }
		ImageInputSplit(Path filename) {
			this.filename = filename.toUri().getPath();
		}
		public void write(DataOutput out) throws IOException { 
			Text.writeString(out, filename); 
		}
		public void readFields(DataInput in) throws IOException { 
			filename = Text.readString(in); 
		}
		public long getLength() { return 0L; }
		public String[] getLocations() { return new String[0]; }
	}

	public InputSplit[] getSplits(JobConf conf, 
				      int numSplits) throws IOException {
		ArrayList<InputSplit> result = new ArrayList<InputSplit>();
		FileSystem fs = FileSystem.get(conf);
		for(Path dir: getInputPaths(conf)) {
			for(FileStatus file: fs.listStatus(dir)) {
				result.add(new ImageInputSplit(file.getPath()));
			}
		}
		return result.toArray(new InputSplit[result.size()]);
	}

	public void validateInput(JobConf conf) { }

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

