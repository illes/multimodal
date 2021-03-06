package bme.iclef.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

// FIXME rename to BinaryFileInputformat
@SuppressWarnings("deprecation")
public class ImageInputFormat extends MultipleFileInputFormat<BytesWritable> {

    public static final String FILES_PER_MAP = "mapreduce.input.imageinputformat.filespermap";

    @Override
    public int getNumFilesPerSplit(JobConf conf) {
	return conf.getInt(FILES_PER_MAP, 1);
    }

    @Override
    public RecordReader<Text, BytesWritable> getRecordReader(InputSplit split,
	    JobConf conf, Reporter reporter) {
	try {
	    return new BinaryFileRecordReader((MultipleFileInputSplit) split, conf,
		    reporter);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

}
