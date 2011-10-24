package bme.iclef.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.Progressable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.VectorWritable;

/**
 * Sets the key as the vector's name.
 * @author Illes Solt
 *
 */
public class VectorNamingOutputFormat extends SequenceFileOutputFormat<Text, VectorWritable> {

    @Override
    public RecordWriter<Text, VectorWritable> getRecordWriter(
            FileSystem ignored, JobConf job, String name, Progressable progress)
            throws IOException {
        
        final RecordWriter<Text, VectorWritable> out = super.getRecordWriter(ignored, job, name, progress);
        
        return new RecordWriter<Text, VectorWritable>() {


	    @Override
	    public void write(Text key, VectorWritable value)
		    throws IOException {
	    	out.write(key, new VectorWritable(new NamedVector(value.get(), key.toString())));
	    }
	    
	    @Override
	    public void close(Reporter reporter) throws IOException {
		out.close(reporter);
		
	    }
	};
    }

    
}
