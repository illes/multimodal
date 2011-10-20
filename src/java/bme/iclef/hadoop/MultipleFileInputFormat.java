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


@SuppressWarnings("deprecation")
public abstract class MultipleFileInputFormat<V> extends FileInputFormat<Text, V> {

    public static class TextArrayWritable extends ArrayWritable {
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

	    for (int i = 0; i < w.length; i++) {
		buffer.append(w[i].toString());
		buffer.append(space);
	    }
	    return buffer.toString();
	}
    }

    public static class MultipleFileInputSplit implements InputSplit {
	private TextArrayWritable list;

	MultipleFileInputSplit() {
	}

	MultipleFileInputSplit(Text[] uris) {
	    this.list = new TextArrayWritable(uris);
	}

	public MultipleFileInputSplit(DataInput in) throws IOException {
	    this.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
	    list.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
	    list = new TextArrayWritable();
	    list.readFields(in);
	}

	@Override
	public long getLength() {
	    return list.get().length;
	}

	/**
	 * Translates the underlying Text[] to String[]
	 */
	public String[] getLocations() {
	    final Writable[] ori = list.get();
	    final String[] locations = new String[ori.length];
	    for (int i = 0; i < locations.length; i++) {
		locations[i] = ((Text) (ori[i])).toString();
	    }
	    return locations;
	}
    }

    @Override
    public InputSplit[] getSplits(JobConf conf, int numSplits)
	    throws IOException {
	ArrayList<InputSplit> result = new ArrayList<InputSplit>();
	/* get the number of files per splits */
	long numFilesPerSplit = getNumFilesPerSplit(conf);
	FileSystem fs = FileSystem.get(conf);

	/*
	 * if it's 0 it means that we want to split whole process into that many
	 * tasks as many map task capacity is there available
	 */
	if (numFilesPerSplit == 0) {
	    long maxMapTasks = new JobClient(conf).getClusterStatus()
		    .getMaxMapTasks();
	    long numFiles = 0;
	    for (Path dir : getInputPaths(conf)) {
		numFiles += fs.listStatus(dir).length;
	    }

	    numFilesPerSplit = numFiles / maxMapTasks;
	}
	
	for (Path dir : getInputPaths(conf)) {
	    List<Text> split = new ArrayList<Text>();
	    for (FileStatus f : fs.listStatus(dir)) {
		if (f.isDir())
		    throw new IllegalStateException("unhandled directory: '" + f.getPath() + "'");
	        Path p = f.getPath();
	        p.getFileSystem(conf); // test
		split.add(new Text(p.toUri().getPath()));
		if (split.size() == numFilesPerSplit) {
		    MultipleFileInputSplit imgSplit = new MultipleFileInputSplit(split
			    .toArray(new Text[split.size()]));
		    result.add(imgSplit);
		    split.clear();
		}
	    }
	    // remainder
	    if (split.size() > 0) {
		MultipleFileInputSplit imgSplit = new MultipleFileInputSplit(split
			.toArray(new Text[split.size()]));
		result.add(imgSplit);
		split.clear();
	    }
	}
	return result.toArray(new InputSplit[result.size()]);
    }

    
    abstract protected int getNumFilesPerSplit(JobConf conf);

    public void validateInput(JobConf conf) {
    }

    public MultipleFileInputFormat() {
	super();
    }

}