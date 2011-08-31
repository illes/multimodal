package bme.iclef.hadoop;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.mahout.clustering.kmeans.Cluster;
import org.apache.mahout.math.Vector;

public class ClusterDumper {

	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
		SequenceFile.Reader reader = ClusterMapper.getSequenFileReader(args[0]);
		WritableComparable<?> key = (WritableComparable<?>) reader.getKeyClass().newInstance();
		Writable value = (Writable) reader.getValueClass().newInstance();
		Writer out = new BufferedWriter(new FileWriter("dump.csv"));
		while (reader.next(key, value)) {
//			System.err.println("KEY:" + reader.getKeyClassName());
//			System.err.println("VALUE:" +reader.getValueClassName());
			Cluster c = (Cluster) value;
			Vector center = c.getCenter();
			
			for (int i = 0; i < center.size(); i++) {
				if (i>0)
					out.write(",");
				out.write(String.valueOf(center.get(i)));
			}
			out.write("\n");
			
//			final Matcher m = ClusterMapper.vectorName.matcher(name);
//			if (!m.matches())
//				throw new IllegalStateException(
//						"unrecognized vector name: '" + name + "'");
//			String sourceName = m.group(1);
//			if (!tfVectors.containsKey(sourceName))
//				tfVectors.put(sourceName, 
//						new NamedVector(new RandomAccessSparseVector(Integer.MAX_VALUE), sourceName));
//			Vector v = tfVectors.get(sourceName);
//			final int cluster = ((IntWritable) key).get();
//			// increment
//			v.set(cluster, v.get(cluster) + 1);
		}
		reader.close();
		out.close();
	}
}
