package bme.iclef.hadoop;

import gnu.trove.TIntIntHashMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.mahout.clustering.WeightedVectorWritable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.utils.vectors.io.VectorWriter;

import bme.iclef.mahout.ByIndexSparseVectorSorter;
import bme.iclef.mahout.Sift2Mahout;
import bme.iclef.mahout.SparseVectorSorter;
import bme.iclef.utils.Maps;

public class ClusterMapper {

	final static double NORMALIZATION_POWER = 1.0;
	final static Pattern vectorName = Pattern.compile("(?i)^.*/([^\\/]*)\\.jp.*sift:?([0-9]+)$");
	

	public static void main(String[] args) throws IOException,
			InstantiationException, IllegalAccessException {


		final Map<String, Vector> tfVectors = new HashMap<String, Vector>(args.length);
		Map<String, String> idLabelMap = null;
		int features = 0;
		{
			boolean expectLabelMapping = false;
			for (String inFile : args) {
				System.err.println("INFO: Processing '" + inFile +"' ...");
				if ("--label-mapping".equals(inFile))
				{
					expectLabelMapping = true;
					continue;
				}
				if (expectLabelMapping)
				{
					idLabelMap = Maps.readStringStringMapFromFile(new File(inFile), Charset.forName("US-ASCII"), ",", new HashMap<String, String>());
					expectLabelMapping = false;
					continue;
				}
				
				
				
				SequenceFile.Reader reader = getSequenFileReader(inFile);
				WritableComparable<?> key = (WritableComparable<?>) reader.getKeyClass().newInstance();
				Writable value = (Writable) reader.getValueClass().newInstance();
				while (reader.next(key, value)) {
					features++;
					//System.err.println(reader.getKeyClassName());
					//System.err.println("Key: " + key + "\tValue: " + value);
					WeightedVectorWritable wvw = (WeightedVectorWritable) value;
					final String name = ((NamedVector) wvw.getVector())
							.getName();
					final Matcher m = vectorName.matcher(name);
					if (!m.matches())
						throw new IllegalStateException(
								"unrecognized vector name: '" + name + "'");
					String sourceName = m.group(1);
					if (!tfVectors.containsKey(sourceName))
						tfVectors.put(sourceName, 
								new NamedVector(new RandomAccessSparseVector(Integer.MAX_VALUE), sourceName));
					Vector v = tfVectors.get(sourceName);
					final int cluster = ((IntWritable) key).get();
					// increment
					v.set(cluster, v.get(cluster) + 1);
				}
				reader.close();
			}
		}
		
		System.err.println("INFO: Done parsing " + features + " features for " + tfVectors.size() +" images.");
		
		System.err.println("INFO: Calculating global frequencies ...");
		
		final TIntIntHashMap clusterTf = new TIntIntHashMap();
		final TIntIntHashMap clusterDf = new TIntIntHashMap();
		int maxClusterIndex = Integer.MIN_VALUE;
		{
			for (Vector tfv : tfVectors.values())
			{
				for (Iterator <Element> iter = tfv.iterateNonZero(); iter.hasNext();) {
					Element e = iter.next();
					clusterTf.adjustOrPutValue(e.index(), (int)e.get(), (int)e.get());					
					clusterDf.adjustOrPutValue(e.index(), 1, 1);
					maxClusterIndex = Math.max(e.index(), maxClusterIndex);
				}
			}
		}

		System.err.println("INFO: Mapping vectors ...");

		ArrayList<Vector> tfIdfVectors = new ArrayList<Vector>();
		tfIdfVectors.ensureCapacity(tfVectors.size());
		{
			for (Vector tfv : tfVectors.values())
			{
				final String name = ((NamedVector)tfv).getName();
				
				int length = 0;
				for (Iterator <Element> iter = tfv.iterateNonZero(); iter.hasNext();)
					length += iter.next().get();
				
				
				final Vector out = new RandomAccessSparseVector(maxClusterIndex+1);
				for (Iterator <Element> iter = tfv.iterateNonZero(); iter.hasNext();) {
					Element e = iter.next();
					double tf = e.get()/length;
					double idf = Math.log((tfVectors.size()+1/*smoothing*/)/(double)clusterDf.get(e.index()));
					//System.err.println("" + e.index() + " " +  tf + " " +clusterDf.get(e.index()));
					out.set(e.index(), tf*idf);
				}
				out.normalize(NORMALIZATION_POWER);
				tfIdfVectors.add(new NamedVector(out, name));
				//System.err.println(out);
			}
		}
		{
			final String outFile = "out.tfidf.vector";
			System.err.println("INFO: Writing tfidf vectors to '"+ outFile + "' ...");
			VectorWriter vectorWriter = Sift2Mahout.getSequenceFileWriter(outFile);
			vectorWriter.write(tfIdfVectors);
			vectorWriter.close();
			System.err.println("INFO: Done writing vectors to '"+ outFile + "'.");
		}
		{
			final String outFile = "out.tf.vector";
			List<Vector> vlist = new ArrayList<Vector>();
			for (Vector v : tfVectors.values())
				vlist.add(new RandomAccessSparseVector(v));
			System.err.println("INFO: Writing tf vectors to '"+ outFile + "' ...");
			final int skip = 100;
			for (int i = 0; i < vlist.size(); i += skip) {
				VectorWriter vectorWriter = Sift2Mahout.getSequenceFileWriter(outFile + String.format(".%05d", i));
				vectorWriter.write(vlist.subList(i, Math.min(vlist.size(), i+skip)));
				vectorWriter.close();
			}
			System.err.println("INFO: Done writing vectors to '"+ outFile + "'.");
		}
		{
			final String outFile = "out.csv";			
			System.err.println("INFO: Writing vectors to CSV '"+ outFile + "' ...");
			final Writer w = new FileWriter(outFile);
			
			int[] clusters = clusterDf.keys();
			Arrays.sort(clusters);
			
			// header
			w.write("id");
			for (int cluster : clusters)
				w.write(",cluster_" + cluster + "_tfidf");
			w.write("\n");
			
			// data
			for (Vector tfidfv : tfIdfVectors)
			{
				final Vector v = tfidfv;
				final String id = ((NamedVector)v).getName();
				w.write(id);
				for(int cluster : clusters)
					w.write("," + (float) v.get(cluster));
				w.write("\n");
			}
			w.close();
			System.err.println("INFO: Done writing vectors to '"+ outFile + "'.");
		}
		// LDA
		// 1. m: number of unique terms in doc
		// 2: label: --
		// 3: sparse (key[int]:value[int])
		{
			final String outFile = "out.lda";
			System.err.println("INFO: Writing vectors to LDA '"+ outFile + "' ...");
			final Writer w = new FileWriter(outFile);
			SparseVectorSorter sorter = new ByIndexSparseVectorSorter();
			// data
			for (Vector tfv : tfVectors.values())
			{
				final String id = ((NamedVector)tfv).getName();
				final String label = idLabelMap == null ? null : idLabelMap.get(id);
//				System.err.println("ID: " + id);
//				System.err.println("LABEL: " + label);
				
				int nonZero = 0;
				for (Iterator iter = tfv.iterateNonZero(); iter.hasNext(); iter.next())
					nonZero++;
				w.write("" + nonZero + " ");
				w.write(label == null ? "?" : label);
//				System.err.println("NZ: " + nonZero);
				
				for (Iterator<Element> iter = sorter.reset(tfv); iter.hasNext();) {
					Element e = iter.next();
					w.write(" " + e.index() + ":" + (int)e.get());
				}
				w.write("\n");
			}
			w.close();
			System.err.println("INFO: Done writing vectors to '"+ outFile + "'.");
		}
		
	}

	static Configuration conf = new Configuration();
	static FileSystem fs; 
	static {
	for (String configFile : new String[] {
//			"/opt/hadoop-0.20.0/conf/core-site.xml", 
//			"/home/hadoop/hadoop-0.20.203.0/conf/hdfs-site.xml", 
//			"/home/hadoop/hadoop-0.20.203.0/conf/core-site.xml"
			})
	{
		if (new File(configFile).exists())
		{
			System.err.println("INFO: Loading config file '" + configFile +"' ...");
			conf.addResource(new Path(configFile));		
		}
	}
	try {
		fs = FileSystem.get(conf);
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
	
	}

	static Reader getSequenFileReader(String inFile)
			throws IOException {
		Path path = new Path(inFile);
		
		Reader r = new Reader(fs, path, conf);
		return r;
	}
}
