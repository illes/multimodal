package bme.iclef.hadoop;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.math.Arrays;

import bme.iclef.hadoop.ImageInputFormat.ImageInputSplit;

/**
 * {@link RecordReader} that expects {@link ImageInputSplit} from
 * {@link ImageInputFormat} and emits key value pairs that correspond to file
 * basename and file contents. File contents are interpreted according to the
 * encoding specified in the job configuration (default: UTF-8).
 * 
 * @author Illes Solt
 * 
 */
@SuppressWarnings("deprecation")
public class TextFileRecordReader implements RecordReader<Text, Text> {

    private int pos;
    final private String[] files;
    final private FileSystem fs;
    final private Charset charset;

    public TextFileRecordReader(ImageInputSplit split, JobConf conf,
	    Reporter reporter) throws IOException {
	files = split.getLocations();
	pos = 0;
	fs = FileSystem.get(conf);
	charset = Charset.forName(conf.get(
		"mapreduce.input.textfilerecordreader.encoding", "UTF-8"));
	System.err.println("DEBUG: c'tor " + Arrays.toString(files) );	
    }

    @Override
    public boolean next(Text key, Text value) throws IOException {
	System.err.println("DEBUG: next" );
	if (pos < files.length) {
	    key.set(files[pos]); // TODO strip path and extension
	    value.set(new String(IOUtils.toByteArray(fs.open(new Path(
		    files[pos]))), charset));
	    System.err.println("DEBUG: next: " + key +" => " + value );
	    pos++;
	    if (pos < files.length) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public Text createKey() {
	return new Text();
    }

    @Override
    public Text createValue() {
	return new Text();
    }

    @Override
    public long getPos() throws IOException {
	return pos;
    }

    public void close() throws IOException {
	pos = files.length;
    }

    public float getProgress() throws IOException {
	return pos / (float) files.length;
    }
}