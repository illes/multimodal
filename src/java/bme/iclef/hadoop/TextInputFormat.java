package bme.iclef.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

@SuppressWarnings("deprecation")
public class TextInputFormat extends MultipleFileInputFormat<Text> {

    public static final String FILES_PER_MAP = "mapreduce.input.imageinputformat.filespermap";

    @Override
    public int getNumFilesPerSplit(JobConf conf) {
	return conf.getInt(FILES_PER_MAP, 1);
    }

    @Override
    public RecordReader<Text, Text> getRecordReader(InputSplit split,
	    JobConf conf, Reporter reporter) {
	try {
	    return new TextFileRecordReader((MultipleFileInputSplit)split, conf, reporter); 
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

}
