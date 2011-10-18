package bme.iclef.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

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
    }

    @Override
    public boolean next(Text key, Text value) throws IOException {
	if (pos >= files.length)
		return false;
	
	key.set(getFileBaseName(files[pos]));
	value.set(new String(IOUtils.toByteArray(fs.open(new Path(files[pos]))), charset));
	pos++;
	return true;
    }

    /**
     * Removes path and extension.
     * @param path
     * @return
     */
    private static String getFileBaseName(String path) {    
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