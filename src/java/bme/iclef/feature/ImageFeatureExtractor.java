package bme.iclef.feature;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.LireFeature;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

import bme.iclef.hadoop.ImageInputFormat;
import bme.iclef.hadoop.ImageOutputWriter;

public class ImageFeatureExtractor {

    private static class ImgFeatures {
	public String source;
	public String vector;
	public int vectLen;
    }

    static Class<? extends LireFeature> forName(String extrMode) {
	if (extrMode.compareToIgnoreCase("cedd") == 0)
	    return net.semanticmetadata.lire.imageanalysis.CEDD.class;
	else if (extrMode.compareToIgnoreCase("fcth") == 0)
	    return net.semanticmetadata.lire.imageanalysis.FCTH.class;
	else if (extrMode.compareToIgnoreCase("tamura") == 0)
	    return net.semanticmetadata.lire.imageanalysis.Tamura.class;
	else if (extrMode.compareToIgnoreCase("edgehist") == 0)
	    return net.semanticmetadata.lire.imageanalysis.EdgeHistogram.class;
	else if (extrMode.compareToIgnoreCase("autocor") == 0)
	    return net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram.class;
	else
	    throw new IllegalArgumentException("Unrecognized name '" + extrMode
		    + "'");
    }
    
    /**
     * Example tring representation:
     * <pre>tamura 18 3.5211693548387095 6.026702360565828 796.0 130.0 163.0 271.0 158.0 188.0 123.0 356.0 177.0 152.0 177.0 328.0 183.0 182.0 141.0 195.0</pre>
     * @param extractor
     * @param img
     * @param v
     * @return
     */
    protected static Vector extactFeatures(LireFeature extractor, BufferedImage img)
    {
	extractor.extract(img);
	final String str = extractor.getStringRepresentation();
	final StringTokenizer st = new StringTokenizer(str, " ");
	
	final String name = st.nextToken();
	final int numFeatures = Integer.parseInt(st.nextToken());
	
	
	Vector v = new DenseVector(numFeatures);
	int i = 0;
	while (st.hasMoreTokens()) {
	    v.set(i++, Double.parseDouble(st.nextToken()));
	}
	if (i!=numFeatures)
	    throw new IllegalStateException("Wrong number of elements in string representation: '" + str +"'");
	return v;
    }

    public static void main(String[] args) throws IOException,
	    InterruptedException, ExecutionException {

	try {
	    ExecutorService es = Executors.newFixedThreadPool(Runtime
		    .getRuntime().availableProcessors());
	    String extrMode = null;
	    Class<? extends LireFeature> extractor = null;
	    try {
		extrMode = args[0];
		extractor = forName(extrMode);
	    } catch (Exception e) {
		e.printStackTrace();
		System.err
			.println("\nSupported methods: cced, fcth, tamura, edgehist, autocor");
		System.err.println("\nExample run:");
		System.err
			.println("\tjava bme.iclef.feature.ImageFeatureExtractor cced test.jpg");
		System.exit(1);
	    }

	    ArrayList<Future<ImgFeatures>> features = new ArrayList<Future<ImgFeatures>>();
	    for (int i = 1; i < args.length; ++i)
		features.add(es.submit(new ImageFeatureExtractorCallable(
			args[i], extractor)));

	    for (int i = 0; i < features.size(); ++i) {
		try {
		    ImgFeatures f = features.get(i).get();

		    if (i == 0) {
			/* print out the header and then the vectors */
			String header = "id,";
			for (int j = 0; j < f.vectLen; ++j) {
			    if (j > 0)
				header += ",";
			    header += extrMode.toLowerCase() + "_" + j;
			}

			System.out.println(header);
		    }

		    System.out.println(f.source + "," + f.vector);
		} catch (Exception e) {
		    System.err.println("WARNING: " + e.getMessage());
		    e.printStackTrace();
		}
	    }

	    es.shutdown();

	} catch (Exception e) {
	    System.err.println("ERROR: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}

    }

    private static class ImageFeatureExtractorCallable implements
	    Callable<ImgFeatures> {
	final String inFile;
	final LireFeature extractor;

	public ImageFeatureExtractorCallable(String inFile,
		Class<? extends LireFeature> extractor)
		throws InstantiationException, IllegalAccessException {
	    this.inFile = inFile;
	    this.extractor = extractor.newInstance();
	}

	@Override
	public ImgFeatures call() throws Exception {
	    // System.err.println("INFO: Processing '" + inFile + "' ...");

	    File fd = new File(inFile);
	    FileInputStream imageStream = new FileInputStream(fd);
	    BufferedImage bImg = ImageIO.read(imageStream);

	    /* create analyser */
	    extractor.extract(bImg);

	    ImgFeatures imFea = new ImgFeatures();
	    imFea.source = fd.getName().replace(".jpg", "");

	    String vect = extractor.getStringRepresentation();
	    String outCSV = vect.replaceAll("[a-z]+ ", "").replaceAll("\\s+",
		    ",");

	    if (extractor instanceof EdgeHistogram) {
		imFea.vector = outCSV.substring(outCSV.indexOf(';') + 1, outCSV
			.length());
		imFea.vectLen = getDim(imFea.vector);
	    } else if (extractor instanceof AutoColorCorrelogram) {
		imFea.vector = outCSV;
		imFea.vectLen = getDim(imFea.vector);
	    } else {
		imFea.vectLen = Integer.parseInt(outCSV.substring(0, outCSV
			.indexOf(',')));
		imFea.vector = outCSV.substring(outCSV.indexOf(',') + 1, outCSV
			.length());
	    }

	    return imFea;
	}

	private int getDim(String vector) {
	    int j = 1;
	    for (int i = 0; i < vector.length(); ++i) {
		if (vector.charAt(i) == ',')
		    ++j;
	    }

	    return j;
	}
    }

    @SuppressWarnings("deprecation")
    public static class Mapper
	    implements
	    org.apache.hadoop.mapred.Mapper<Text, BytesWritable, Text, VectorWritable> {

	public static final String EXTRACTOR_NAME = "mapreduce.map.lirefeatureextractor.extractor.name";
	LireFeature extractor = null;

	@Override
	public void map(Text key, BytesWritable value,
		OutputCollector<Text, VectorWritable> output, Reporter reporter)
		throws IOException {
	    final BufferedImage img = ImageIO.read(new ByteArrayInputStream(
		    value.getBytes(), 0, value.getLength()));

	    // FIXME replace null with Vector
	    Vector features = ImageFeatureExtractor.extactFeatures(extractor, img);
	    
	    output.collect(key, new VectorWritable(features));
	}

	/**
	 * Run a lire job.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

	    if (args.length != 2) {
		System.err
			.println("Usage: \n java ...  <input_dir> <output_dir>");
	    }

	    JobConf conf = new JobConf();
	    conf.setJobName("liretest");

	    conf.setInputFormat(ImageInputFormat.class);
	    conf.set(ImageInputFormat.FILES_PER_MAP, String.valueOf(10));
	    

	    conf.setMapperClass(ImageFeatureExtractor.Mapper.class);
	    conf.set(ImageFeatureExtractor.Mapper.EXTRACTOR_NAME, "tamura");

	    conf.setMapOutputKeyClass(Text.class);
	    conf.setMapOutputValueClass(VectorWritable.class);

	    // conf.setReducerClass(IdentityReducer.class);

	    conf.setOutputKeyClass(Text.class);
	    conf.setOutputValueClass(VectorWritable.class);

	    conf.setOutputFormat(ImageOutputWriter.class);

	    FileInputFormat.setInputPaths(conf, new Path(args[0]));
	    FileOutputFormat.setOutputPath(conf, new Path(args[1]));

	    JobClient.runJob(conf);
	}

	@Override
	public void configure(JobConf job) {
	    try {
		extractor = forName(
			job
				.get(
					EXTRACTOR_NAME,
					"cedd")).newInstance();
	    } catch (InstantiationException e) {
		throw new RuntimeException(e);
	    } catch (IllegalAccessException e) {
		throw new RuntimeException(e);
	    }
	}

	@Override
	public void close() throws IOException {
	    extractor = null;
	}

    }
}