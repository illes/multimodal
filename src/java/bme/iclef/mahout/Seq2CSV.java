package bme.iclef.mahout;

import java.util.Iterator;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

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
			
			String outName = inFile.substring (0, inFile.indexOf ('.'));
			
			BufferedWriter out = new BufferedWriter(new FileWriter(outName+".csv"));
			
			SequenceFile.Reader seqReader = new SequenceFile.Reader (fs, path, conf);

			VectorWritable vw = new VectorWritable ();
			LongWritable k = new LongWritable ();

			while (seqReader.next (k, vw)) {
				Iterator<Vector.Element> it = vw.get ().iterator ();
				int i = 0;
			
				while (it.hasNext ()) {
					i++;
					Double val = new Double (it.next ().get ());
					out.write (val.toString ());
					if (i < 128) {
						out.write (",");
					}
				}
			
				if (i != 128)
					System.out.println ("we have a problem: "+ i);
			
				out.write ("\n");
			}
			out.close();
		}
	}
	
}