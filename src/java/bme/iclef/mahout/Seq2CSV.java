package bme.iclef.mahout;

import java.util.Iterator;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.utils.vectors.io.SequenceFileVectorWriter;
import org.apache.mahout.utils.vectors.io.VectorWriter;

public class Seq2CSV {
	public static void main(String[] args) throws IOException {
		Configuration conf = new Configuration ();
		FileSystem fs = FileSystem.get (conf);
	
		for (String inFile : args) {
			Path path = new Path (inFile);

			SequenceFile.Reader seqReader = new SequenceFile.Reader (fs, path, conf);

			VectorWritable vw = new VectorWritable ();
			LongWritable k = new LongWritable ();

			while (seqReader.next (k, vw)) {
				Iterator<Vector.Element> it = vw.get ().iterator ();
				int i = 0;
			
				while (it.hasNext ()) {
					i++;
					System.out.print (it.next ().get ());
					if (i < 128) {
						System.out.print (",");
					}
				}
			
				if (i != 128)
					System.out.println ("we have a problem: "+ i);
			
				System.out.println ();
			}
		}
	}
	
}