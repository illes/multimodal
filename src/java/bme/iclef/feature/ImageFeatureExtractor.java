package bme.iclef.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import net.semanticmetadata.lire.imageanalysis.*;

public class ImageFeatureExtractor {

	private static class ImgFeatures {
		public String source;
		public String vector;
		public int vectLen;
	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException {

		try {
			ExecutorService es = Executors.newFixedThreadPool(Runtime
					.getRuntime().availableProcessors());

      String extrMode = args[0];

      String extrType = "net.semanticmetadata.lire.imageanalysis.CEDD";
      if (extrMode.compareToIgnoreCase ("cedd") == 0)
        extrType = "net.semanticmetadata.lire.imageanalysis.CEDD";
      else if (extrMode.compareToIgnoreCase ("fcth") == 0)
        extrType = "net.semanticmetadata.lire.imageanalysis.FCTH";
      else if (extrMode.compareToIgnoreCase ("tamura") == 0)
			  extrType = "net.semanticmetadata.lire.imageanalysis.Tamura";
			else if (extrMode.compareToIgnoreCase ("edgehist") == 0)
        extrType = "net.semanticmetadata.lire.imageanalysis.EdgeHistogram";
			else if (extrMode.compareToIgnoreCase ("autocor") == 0)
			  extrType = "net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram";
			else {
			  System.err.println ("ERROR: Unsupported feature extraction method!");
			  System.err.println ("ERROR: " + extrMode + " unknown");
			  System.err.println ("\nSupported methods: cced, fcth, tamura, edgehist, autocor");
			  System.err.println ("\nExample run:");
			  System.err.println ("\tjava bme.iclef.feature.ImageFeatureExtractor cced test.jpg");
			  System.exit (1);
			}

			ArrayList<Future<ImgFeatures>> features = new ArrayList<Future<ImgFeatures>>();
			for (int i = 1; i < args.length; ++i)
				features.add(es
						.submit(new ImageFeatureExtractorCallable(args[i],extrType)));

			for (int i = 0; i < features.size (); ++i) {
				try {
					ImgFeatures f = features.get (i).get ();

					if (i == 0) {
					  /* print out the header and then the vectors */
            String header = "id,";
            for (int j = 0; j < f.vectLen; ++j) {
              if (j > 0) header += ",";
              header += extrMode.toLowerCase ()+"_"+j;
            }

            System.out.println (header);
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
		final String extrType;

		public ImageFeatureExtractorCallable (String inFile, String extrType) {
			this.inFile = inFile;
			this.extrType = extrType;
		}

		@Override
		public ImgFeatures call() throws Exception {
			// System.err.println("INFO: Processing '" + inFile + "' ...");

			File fd = new File(inFile);
			FileInputStream imageStream = new FileInputStream(fd);
			BufferedImage bImg = ImageIO.read(imageStream);
      
			/* create analyser */
			LireFeature fExtractor = (LireFeature) Class.forName(extrType).newInstance ();
			fExtractor.extract (bImg);

			ImgFeatures imFea = new ImgFeatures();
			imFea.source = fd.getName ().replace (".jpg", "");
			
			String vect = fExtractor.getStringRepresentation ();
			String outCSV = vect.replaceAll("[a-z]+ ", "").replaceAll(
					"\\s+", ",");
      
      if (extrType.contains ("EdgeHistogram")) {
        imFea.vector = outCSV.substring (outCSV.indexOf(';')+1, outCSV.length());
        imFea.vectLen = getDim (imFea.vector);
      } else if (extrType.contains ("AutoColorCorrelogram")) {
        imFea.vector = outCSV;
        imFea.vectLen = getDim (imFea.vector);
      } else {
        imFea.vectLen = Integer.parseInt (outCSV.substring (0, outCSV.indexOf(',')));
        imFea.vector = outCSV.substring (outCSV.indexOf(',')+1, outCSV.length());
      }
      
			return imFea;
		}
		
		private int getDim (String vector) {
		  int j = 1;
		  for (int i = 0; i < vector.length (); ++i) {
		    if (vector.charAt (i) == ',') ++j;
		  }
		  
		  return j;
		}
	}
}