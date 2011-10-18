package bme.iclef.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.math.Arrays;

@SuppressWarnings("deprecation")
public class ImageInputFormat extends FileInputFormat<Text, Text> {

	public static final String FILES_PER_MAP = 
		    "mapreduce.input.imageinputformat.filespermap";


	static class TextArrayWritable extends ArrayWritable {
		public TextArrayWritable() {
			super(Text.class);
		}

		public TextArrayWritable(Text[] texts) {
		    super(Text.class, texts);
		}

		@Override
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
			this.list = new TextArrayWritable (uris);
	       	}

		public ImageInputSplit(DataInput in) throws IOException {
		    	this.readFields(in);
		}

		@Override
		public void write(DataOutput out) throws IOException { 
			list.write (out);
		}

		@Override
		public void readFields(DataInput in) throws IOException { 
			list = new TextArrayWritable ();
			list.readFields (in);
		}

		@Override
		public long getLength() { return list.get().length; }
		
		
		/**
		 * Translates the underlying Text[] to String[]
		 */
		public String[] getLocations() { 
		    final Writable[] ori = list.get();
		    final String[] locations = new String[ori.length];
		    for (int i = 0; i < locations.length; i++) {
			locations[i] = ((Text)(ori[i])).toString();
		    }
		    return locations;
		}
	}

	@Override
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
		System.err.println("DEBUG: numfilespersplit: " + numFilesPerSplit);					
		
        		for(Path dir: getInputPaths(conf)) {
        			List<Text> split = new ArrayList<Text> ();
        			for (FileStatus f: fs.listStatus (dir)) {
        				split.add (new Text (f.getPath ().toUri().getPath()));
					System.err.println("DEBUG: last: " + split.get(split.size()-1).toString());	        				
        				if (split.size() == numFilesPerSplit) {
        					ImageInputSplit imgSplit = 
        						new ImageInputSplit (split.toArray (new Text[split.size ()]));
        					System.err.println("DEBUG: split length: " + imgSplit.getLocations().length);	        				        					
        					System.err.println("DEBUG: split: " + Arrays.toString(imgSplit.getLocations()));					
        					result.add (imgSplit);
        					split.clear ();
        				}
        			}
        		}
		System.err.println("DEBUG: " + Arrays.toString(result.toArray()));
		return result.toArray(new InputSplit[result.size()]);
	}

	public void validateInput(JobConf conf) { }

	public static long getNumFilesPerSplit(JobConf conf) {
		return conf.getInt(FILES_PER_MAP, 1);
	}

	@Override
	public RecordReader<Text, Text> getRecordReader(InputSplit split,
							       JobConf conf, 
							       Reporter reporter) {
	    try {
		return new TextFileRecordReader((ImageInputSplit)split, conf, reporter);
	    } catch (IOException e) {
		throw new RuntimeException(e);
	    }
	}

}

