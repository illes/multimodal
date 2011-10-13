package bme.iclef.hadoop;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.LineRecordReader;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import bme.iclef.hadoop.ImageInputFormat.ImageInputSplit;
import bme.iclef.hadoop.ImageInputFormat.TextArrayWritable;
import bme.iclef.utils.FileUtils;

/**
 * {@link RecordReader} that expects {@link ImageInputSplit} from
 * {@link ImageInputFormat} and emits key value pairs that correspond to file
 * basename and file contents according to the encoding specified in the job
 * configuration (default: UTF-8).
 * 
 * @author illes
 * 
 */
@SuppressWarnings("deprecation")
public class TextFileRecordReader implements RecordReader<Text, Text> {

    private int pos;
    final private String[] files;
    private FileSystem fs;
    private Charset charset;

    public TextFileRecordReader(InputStream in, long offset, long endOffset,
	    Configuration conf) throws IOException {
	ImageInputSplit split = new ImageInputSplit();
	split.readFields(new DataInputStream(in));
	files = split.getLocations();
	pos = 0;
	fs = FileSystem.get(conf);
	charset = Charset.forName(conf.get("multimodal.textfile.encoding",
		"UTF-8"));
    }

    @Override
    public boolean next(Text key, Text value) throws IOException {

	while (pos < files.length) {
	    key.set(files[pos]); // TODO strip path and extension
	    value.set(new String(IOUtils.toByteArray(fs.open(new Path(
		    files[pos]))), charset));

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