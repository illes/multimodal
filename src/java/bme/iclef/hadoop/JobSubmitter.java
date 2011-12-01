package bme.iclef.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;

public class JobSubmitter {

    final static String PROPERTY = "mapred.job.file";
    public static void main(String[] args) throws FileNotFoundException, IOException {
	String jobConfFile = System.getProperty(PROPERTY);
	if (jobConfFile == null)
	    throw new NullPointerException(PROPERTY + " not defined (use -D)");
	
	JobConf conf = new JobConf(new Path(jobConfFile));
	JobClient jc = new JobClient(conf);
	RunningJob rj = jc.submitJob(conf);
	System.err.println("Job ID:\t" + rj.getID());
	System.err.println("Tracking URL:\t" + rj.getTrackingURL());
    }
}
