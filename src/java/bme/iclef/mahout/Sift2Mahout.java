package bme.iclef.mahout;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.utils.vectors.io.SequenceFileVectorWriter;
import org.apache.mahout.utils.vectors.io.VectorWriter;

public class Sift2Mahout {

	private static final int numPatches = 128;
	private static final Pattern separator = Pattern.compile(" ");
	private static final boolean useNamedVectors = true;

	private static class SiftResult {
		public String source;
		public Collection<Vector> vectors;
		
		void writeSequenceFile() throws IOException
		{
			Sift2Mahout.writeSequenceFile(source + ".seq", vectors);			
		}
	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException {

		try {
			ExecutorService es = Executors.newFixedThreadPool(Runtime
					.getRuntime().availableProcessors());
			//List<Future<SiftResult>> futures = new ArrayList<Future<SiftResult>>();
//			for (String inFile : args)
//				futures.add(es.submit(new Sift2MahoutCallable(inFile)));

//			for (Future<SiftResult> future : futures) {
//				SiftResult sift = future.get();
//				writeSequenceFile(sift.source + ".seq", sift.vectors);
//			}
			for (String inFile : args)
				es.submit(new Sift2MahoutCallable(inFile));
			
			es.shutdown();
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static VectorWriter getSequenceFileWriter(String outFile)
			throws IOException {
		Path path = new Path(outFile);
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		SequenceFile.Writer seqWriter = SequenceFile.createWriter(fs, conf,
				path, LongWritable.class, VectorWritable.class);

		return new SequenceFileVectorWriter(seqWriter);
	}
	
	public static void writeSequenceFile(String outFile, Collection<Vector> vectors) throws IOException
	{
		VectorWriter vectorWriter = getSequenceFileWriter(outFile);
		long numDocs = vectorWriter.write(vectors);
		if (numDocs != vectors.size())
			throw new IllegalStateException("" + numDocs + "!="
					+ vectors.size());

		vectorWriter.close();		
	}

	private static class Sift2MahoutCallable implements Callable<SiftResult> {

		final String inFile;

		public Sift2MahoutCallable(String inFile) {
			this.inFile = inFile;
		}

		@Override
		public SiftResult call() throws Exception {
			System.err.println("INFO: Processing '" + inFile + "' ...");

			List<Vector> vl = new ArrayList<Vector>();

			BufferedReader r = new BufferedReader(new FileReader(inFile));
			String line;
			int lineNo = 1;
			while ((line = r.readLine()) != null) {

				String[] columns = separator.split(line);
				if (columns.length != numPatches)
					throw new IllegalStateException("Line " + inFile + ":"
							+ lineNo + " has " + columns.length
							+ " columns, expected " + numPatches + " ('" + line
							+ "').");

				Vector v = new DenseVector(numPatches);
				for (int i = 0; i < columns.length; i++)
					v.set(i, Double.parseDouble(columns[i]));

				if (useNamedVectors)
					vl.add(new NamedVector(v, inFile + ":" + lineNo));
				else 
					vl.add(v);
				
				lineNo++;

			}
			SiftResult out = new SiftResult();
			out.source = inFile;
			out.vectors = vl;
			
			out.writeSequenceFile();
			
			return out;
		}
	}
}
