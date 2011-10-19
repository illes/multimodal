package bme.iclef.hadoop;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import bme.iclef.hadoop.MultipleFileInputFormat.MultipleFileInputSplit;

@SuppressWarnings("deprecation")
public abstract class MultipleFileRecordReader<V> implements
	RecordReader<Text, V> {

    protected int pos;
    protected final String[] files;
    protected final FileSystem fs;

    public MultipleFileRecordReader(MultipleFileInputSplit split, JobConf conf,
	    Reporter reporter) throws IOException {
	files = split.getLocations();
	pos = 0;
	fs = FileSystem.get(conf);
    }

    /**
     * Removes path and extension.
     * 
     * @param path
     * @return
     */
    public static String getFileBaseName(String path) {
	/* remove path */
	String baseName = new File(path).getName();

	/* remove extension */
	final int extPos = baseName.lastIndexOf('.');

	if (extPos >= 0)
	    return baseName.substring(0, extPos);
	else
	    return baseName;
    }

    @Override
    abstract public boolean next(Text key, V value) throws IOException;

    @Override
    public Text createKey() {
	return new Text();
    }

    @Override
    public long getPos() throws IOException {
	return pos;
    }

    @Override
    public void close() throws IOException {
	pos = files.length;
    }

    @Override
    public float getProgress() throws IOException {
	return pos / (float) files.length;
    }

    /**
     * Sets the key to the basename of the next file, and returns its full path.
     * @param key the key to be set
     * @return the next file's path or <code>null</code> iff at EOF.
     * @see {@link #getFileBaseName(String)}
     */
    protected String next(Text key) {
	if (pos >= files.length)
	    return null;

	final String path = files[pos++];
	key.set(getFileBaseName(path));
	return path;
    }

}