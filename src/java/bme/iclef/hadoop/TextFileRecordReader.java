package bme.iclef.hadoop;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import bme.iclef.hadoop.MultipleFileInputFormat.MultipleFileInputSplit;


/**
 * {@link RecordReader} that expects {@link MultipleFileInputSplit} from a
 * {@link MultipleInputFormat} and emits key value pairs that correspond to file
 * basename and text file contents. File contents are interpreted according to the
 * encoding specified in the job configuration (default: UTF-8).
 * 
 * @author Illes Solt
 * 
 */
@SuppressWarnings("deprecation")
public class TextFileRecordReader extends MultipleFileRecordReader<Text> {

    final Charset charset;

    public TextFileRecordReader(MultipleFileInputSplit split, JobConf conf,
	    Reporter reporter) throws IOException {
	super(split, conf, reporter);
	charset = Charset.forName(conf.get(
		"mapreduce.input.textfilerecordreader.encoding", "UTF-8"));
    }
    
    @Override
    public boolean next(Text key, Text value) throws IOException {
	final String path = super.next(key);
        if (path == null)
        	return false;
        
        value.set(new String(IOUtils.toByteArray(fs.open(new Path(path))), charset));
        return true;
    }

    @Override
    public Text createValue() {
	return new Text();
    }
}