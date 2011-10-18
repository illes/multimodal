package bme.iclef.predict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.hadoop.fs.Path;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import au.com.bytecode.opencsv.CSVWriter;
import bme.iclef.hadoop.ImageInputFormat;
import bme.iclef.hadoop.ImageOutputWriter;
import bme.iclef.predict.Prediction.Label;
import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;
import bme.iclef.utils.FileUtils;
import bme.iclef.xml.XmlParser;



@SuppressWarnings("deprecation")
public class RegexPredictor extends Predictor {

	private final Map<Matcher, Prediction> rules = new HashMap<Matcher, Prediction>();
	private final CSVWriter csv;
	private String[] csvRow;

	public RegexPredictor() throws IOException {
	    this(new FileWriter(RegexPredictor.class.getSimpleName() + ".csv"));
	}
	public RegexPredictor(Writer writer) throws IOException {
	    this(writer, false, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
	}

	public RegexPredictor(Writer writer, boolean omitHeader, char separator, char quote) {
		
		// X ray
		addRule("\\bX[- ]?ray", Label.XR_XRay, 0.8f);
		addRule("\\b(DXA|DEXA|BMD)\\b", Label.XR_XRay, 0.8f);
		addRule("(?i)\\b(radiograph)", Label.XR_XRay, 0.8f);
		
		// angiography
		addRule("(?i)\\b(micro-?)?(a(ng|rter)iogra\\w+)", Label.XR_XRay, 0.8f);
		
		//CLUES:
		// (para-?)sagittal
		// image
		// imaging
		// lesion
		// scan
		
		
		// ultasound
		addRule("\\b(US)[ -]?imag\\w*\\b", Label.US_Ultrasound, 0.8f);
		addRule("(?i)\\b(ultra.?sonic|ultra.?sound)s?\\b", Label.US_Ultrasound, 0.9f);
		addRule("(?i)\\b(sonogram)s?\\b", Label.US_Ultrasound, 0.9f);
		addRule("(?i)\\b(brightness scan|echocardiograph\\w*)s?\\b", Label.US_Ultrasound, 0.9f);
		
		// MRI
		addRule("\\b([fN]?MR[IT])s?\\b", Label.MR_MagneticResonance, 0.9f);
		addRule("(?i)\\b((Magnetic resonance|MR) imag\\w+)s?\\b", Label.MR_MagneticResonance, 0.7f);
		addRule("\\b(T[12].?weight\\w*)\\b", Label.MR_MagneticResonance, 0.8f);
		
		// CT
		addRule("\\b(CT)s?\\b", Label.CT_ComputedTomography, 0.8f);
		addRule("\\b(?i)(comput\\w+.?tomograph\\w+)\\b", Label.CT_ComputedTomography, 0.9f);
		
		// endoscope
		addRule("(?i)\\b(endoscop\\w*)\\b", Label.EN_Endoscope, 0.9f);
		
		
		// photos
		addRule("(?i)\\b(photo(?!-)|photograph\\w*)s?\\b", Label.PX_Photo, 0.9f);
		addRule("(?i)\\b(picture)s?\\b", Label.PX_Photo, 0.6f);
		addRule("(?i)operat", Label.PX_Photo, 0.3f);
		addRule("\\b(were taken)\\b", Label.PX_Photo, 0.7f);
		addRule("(?i)\\b(courtesy)\\b", Label.PX_Photo, 0.3f);
		addRule("(?i)\\b(((ao)?slo|MPE|TPEF|SHG|MPLSM)[ -]?imag\\w+)\\b", Label.UnspecifiedPhotograph, 0.9f);
		addRule("(?i)\\b(diffus\\w+ illuminat\\w+)\\b", Label.UnspecifiedPhotograph, 0.9f);

		addRule("(?i)\\b(immuno-?staining)\\b", Label.HX_Histopathology, 0.9f);
		addRule("(?i)\\b(histolog[yi]\\w*|immuno\\W*histo\\W*chemic\\w*|histopatholog\\w*)\\b", Label.HX_Histopathology, 0.9f);
		addRule("(?i)\\b((?:tumor|spindle) cell)s?\\b", Label.HX_Histopathology, 0.7f);
		
		
		addRule("(?i)\\b(ophtalmo\\w+)\\b", Label.RN_Retinograph, 0.9f);
		addRule("(?i)\\b(optic disc|fundus|retinopathy)\\b", Label.RN_Retinograph, 1f);

		addRule("(?i)\\b(h[yi]per\\W*pigment\\w+)\\b", Label.DM_Dermatology, 1f);
		addRule("(?i)\\b(skin|rash|lesion|scar)\\b", Label.DM_Dermatology, 0.7f);
		
		// microscope
		addRule("(?i)\\b(magnification)\\b", Label.UnspecifiedMicroscopy, 0.6f);
		addRule("(?i)\\b(\\w*oscop(y|ic))\\b", Label.UnspecifiedMicroscopy, 0.4f);
		addRule("(?i)\\b(microscop\\w*)\\b", Label.UnspecifiedMicroscopy, 0.9f);
		
		// Plots
		//addRule("(?i)\\b(bar|(box|profile|eisen|dot|scatter).?plot|histogram|curve|chart)s?\\b", Label.Graphs, 0.8f);
		addRule("(?i)\\b(error bars?)\\b", Label.GX_Graphs, 0.8f);
		addRule("(?i)\\b([xyz].?axis|logarithmic)\\b", Label.GX_Graphs, 0.8f);
		addRule("(?i)\\b(standard deviation|mean|p.?value)s?\\b", Label.GX_Graphs, .6f);		
		addRule("(?i)\\b(visualization|diagram|phylogram|dendogram)s?\\b", Label.GX_Graphs, 1.0f);
		addRule("(?i)\\b(graphic\\w+ represent\\w+)\\b", Label.GX_Graphs, 1.0f);
		addRule("(?i)\\b(plot(ted)?|alignment|tree)s?\\b", Label.GX_Graphs, 0.4f);
		addRule("(?i)\\b(distribution|frequenc\\w+|algorithm)s?\\b", Label.GX_Graphs, 0.6f);

		// Other
		addRule("(?i)\\b(schema(tic)?)\\b", Label.DR_Drawing, 0.8f);
		addRule("(?i)\\b(map|network|topolog\\w*)\\b", Label.DR_Drawing, 0.7f);
		addRule("(?i)\\b(cartoon|((state|flow).?)?(?:chart|diagram))s?\\b", Label.DR_Drawing, 1f);
		addRule("(?i)\\b(illustrat\\w+)\\b", Label.DR_Drawing, 0.5f);
		addRule("(?i)\\b(screen.?shot)\\b", Label.UnspecifiedGraphic, 1.0f);
		addRule("(?i)\\b(hyper.?link)\\b", Label.UnspecifiedGraphic, 1.0f);
		addRule("(?i)\\b(user|application|interface)\\b", Label.UnspecifiedGraphic, 0.6f);
		
		// blots
		addRule("(?i)\\b((immuno)?blot|lane)s?\\b", Label.GL_Gel, 1.0f);
		
		addRule("\\b3D\\b", Label.forCode("3D"), 1.1);
		addRule("(?i)\\b(spatial|three\\W+dimension\\w*|Structur\\w+\\W+(?:alignment|motif)|space filling|side[ -]?chain|\\w*(?:helix|helical)\\w*)s?\\b", Label.forCode("3D"), 0.9);
		addRule("(?i)\\b(structur\\w+)\\b", Label.forCode("3D"), 0.7);
		
		// debug
		addRule("(?i)\\bImmuno-?localisation\\b", Label.HX_Histopathology, 1);
		addRule("(?i)\\bsheets?\\b", Label.HX_Histopathology, 0.6);
		
		addRule("(?i)\\b(gel)\\b", Label.GL_Gel, 1f);
		addRule("\\bPCR\\b", Label.forCode("GL"), 1);	// word_GFP_binarized = 1: FL (5.0/1.0)
		addRule("(?i)\\b(expression)\\b", Label.GL_Gel, 0.7f);
		addRule("(?i)\\b(?:graft\\w*)\\b", Label.GR_GrossPathology, 0.8);	// word_mass_binarized = 1: GR (8.0/4.0)
		
		addRule("(?i)\\b(angiogra\\w*|dilatation)\\b", Label.AN_Angiography, 0.9f);
		addRule("(?i)\\b(arter\\w*)\\b", Label.AN_Angiography, 0.7f);

		addRule("(?i)\\b(electron[ -]?micro\\w*|dark field)\\b", Label.EM_ElectronMicroscopy, 1f);
		addRule("(?i)\\b(scanning)\\b", Label.EM_ElectronMicroscopy, 0.9f);
		addRule("\\b(S?EM)\\b", Label.EM_ElectronMicroscopy, 0.95f);

		addRule("(?i)fluorescen", Label.FL_Fluorescense, 1);	// word_fluorescent_binarized = 1: FL (5.0)
		
		addRule("(?i)\\b(colonoscop\\w*)\\b", Label.EN_Endoscope, 0.9f);

		// weka
		
		addRule("(?i)\\bmass\\b", Label.GR_GrossPathology, 0.67);	// word_mass_binarized = 1: GR (8.0/4.0)
		addRule("(?i)\\blateral\\b", Label.PX_Photo, 0.7);	// word_lateral_binarized = 1: PX (7.0/3.0)
		addRule("(?i)\\bobtained\\b", Label.GX_Graphs, 0.71);	// word_obtained_binarized = 1: GX (5.0/2.0)
		addRule("(?i)\\barrow\\b", Label.US_Ultrasound, 0.62);	// word_arrow_binarized = 1: US (8.0/5.0)
		addRule("(?i)\\bnumber\\b", Label.GX_Graphs, 0.89);	// word_number_binarized = 1: GX (8.0/1.0)
		addRule("(?i)\\bMean\\b", Label.GX_Graphs, 1);	// word_Mean_binarized = 1: GX (5.0)
		addRule("(?i)\\beffect\\b", Label.forCode("GX"), 0.83);	// word_effect_binarized = 1: GX (5.0/1.0)
		addRule("(?i)\\bdistribution\\b", Label.forCode("GX"), 0.86);	// word_distribution_binarized = 1: GX (6.0/1.0)
		addRule("(?i)\\bcourse\\b", Label.forCode("GX"), 0.83);	// word_course_binarized = 1: GX (5.0/1.0)
		addRule("<", Label.forCode("GX"), 0.92);	// word_<_binarized = 1: GX (12.0/1.0)
		addRule("(?i)\\brespectively\\b", Label.forCode("GX"), 0.79);	// word_respectively_binarized = 1: GX (11.0/3.0)
		addRule("(?i)\\bmean\\b", Label.forCode("GX"), 1);	// word_mean_binarized = 1: GX (16.0)
		addRule("(?i)\\bline\\b", Label.forCode("GX"), 0.91);	// word_line_binarized = 1: GX (21.0/2.0)
		addRule("(?i)\\bcyst\\b", Label.forCode("GR"), 0.75);	// word_cyst_binarized = 1: GR (6.0/2.0)
		addRule("(?i)\\bbladder\\b", Label.forCode("XR"), 0.62);	// word_bladder_binarized = 1: XR (5.0/3.0)
		addRule("(?i)\\banalysis\\b", Label.forCode("GX"), 0.62);	// word_analysis_binarized = 1: GX (5.0/3.0)
		addRule("(?i)\\bremoved\\b", Label.forCode("GR"), 0.67);	// word_removed_binarized = 1: GR (8.0/4.0)
		addRule("(?i)\\bimage\\b", Label.forCode("GX"), 0.58);	// word_image_binarized = 1: GX (7.0/5.0)
		addRule("(?i)\\bvein\\b", Label.forCode("GR"), 0.71);	// word_vein_binarized = 1: GR (5.0/2.0)
		addRule("(?i)\\bfemur\\b", Label.forCode("XR"), 0.71);	// word_femur_binarized = 1: XR (5.0/2.0)
		addRule("(?i)\\bstain\\b", Label.forCode("HX"), 0.88);	// word_stain_binarized = 1: HX (7.0/1.0)
		addRule("(?i)\\bbiopsy\\b", Label.forCode("HX"), 0.71);	// word_biopsy_binarized = 1: HX (5.0/2.0)
		addRule("(?i)\\bimages\\b", Label.forCode("CM"), 0.6);	// word_images_binarized = 1: CM (9.0/6.0)
		addRule("(?i)\\bfocal\\b", Label.forCode("HX"), 0.67);	// word_focal_binarized = 1: HX (6.0/3.0)
		addRule("(?i)\\bcytoplasm\\b", Label.forCode("HX"), 1);	// word_cytoplasm_binarized = 1: HX (7.0)
		addRule("(?i)\\bimmunostaining\\b", Label.forCode("HX"), 0.75);	// word_immunostaining_binarized = 1: HX (6.0/2.0)
		addRule("(?i)\\bScale\\b", Label.forCode("HX"), 0.73);	// word_Scale_binarized = 1: HX (8.0/3.0)
		addRule("(?i)\\bmagnification\\b", Label.forCode("HX"), 0.91);	// word_magnification_binarized = 1: HX (10.0/1.0)
		addRule("(?i)\\blung\\b", Label.forCode("CT"), 0.62);	// word_lung_binarized = 1: CT (5.0/3.0)
		addRule("(?i)\\bstaining\\b", Label.forCode("HX"), 0.89);	// word_staining_binarized = 1: HX (24.0/3.0)
		addRule("(?i)\\bsize\\b", Label.forCode("PX"), 0.58);	// word_size_binarized = 1: PX (7.0/5.0)
		addRule("(?i)\\bcm\\b", Label.forCode("PX"), 0.67);	// word_cm_binarized = 1: PX (8.0/4.0)
		addRule("(?i)\\bedges\\b", Label.forCode("DR"), 0.71);	// word_edges_binarized = 1: DR (5.0/2.0)
		addRule("(?i)\\bfracture\\b", Label.forCode("XR"), 0.83);	// word_fracture_binarized = 1: XR (5.0/1.0)
		addRule("\\bGFP\\b", Label.forCode("FL"), 0.83);	// word_GFP_binarized = 1: FL (5.0/1.0)
		addRule("\\bDNA\\b", Label.forCode("GL"), 0.6);	// word_GFP_binarized = 1: FL (5.0/1.0)
		addRule("(?i)\\bWestern\\b", Label.forCode("GL"), 0.71);	// word_Western_binarized = 1: GL (5.0/2.0)
		addRule("(?i)\\binteraction\\b", Label.forCode("DR"), 0.71);	// word_interaction_binarized = 1: DR (5.0/2.0)
		addRule("(?i)\\bRT\\b", Label.forCode("GL"), 0.71);	// word_RT_binarized = 1: GL (10.0/4.0)
		addRule("(?i)\\bpathway\\b", Label.forCode("DR"), 0.88);	// word_pathway_binarized = 1: DR (7.0/1.0)
		addRule("(?i)\\bray\\b", Label.forCode("XR"), 0.81);	// word_ray_binarized = 1: XR (13.0/3.0)
		addRule("(?i)\\bPlain\\b", Label.forCode("XR"), 1);	// word_Plain_binarized = 1: XR (5.0)
		addRule("(?i)\\bfluorescent\\b", Label.forCode("FL"), 1);	// word_fluorescent_binarized = 1: FL (5.0)
		addRule("(?i)\\bDAPI\\b", Label.forCode("FL"), 0.89);	// word_DAPI_binarized = 1: FL (8.0/1.0)
		addRule("(?i)\\bdisc\\b", Label.forCode("RN"), 0.71);	// word_disc_binarized = 1: RN (5.0/2.0)
		addRule("(?i)\\bLV\\b", Label.forCode("US"), 0.88);	// word_LV_binarized = 1: US (7.0/1.0)
		addRule("(?i)\\bartery\\b", Label.forCode("AN"), 0.69);	// word_artery_binarized = 1: AN (9.0/4.0)
		addRule("(?i)\\bconfocal\\b", Label.forCode("FL"), 1);	// word_confocal_binarized = 1: FL (8.0)
		addRule("(?i)\\bultrasound\\b", Label.forCode("US"), 1);	// word_ultrasound_binarized = 1: US (5.0)
		addRule("(?i)\\bradiograph\\b", Label.forCode("XR"), 1);	// word_radiograph_binarized = 1: XR (16.0)
		addRule("(?i)\\bScanning\\b", Label.forCode("EM"), 0.86);	// word_Scanning_binarized = 1: EM (6.0/1.0)
		addRule("(?i)\\bbp\\b", Label.forCode("GL"), 1);	// word_bp_binarized = 1: GL (15.0)
		addRule("(?i)\\btomography\\b", Label.forCode("CT"), 1);	// word_tomography_binarized = 1: CT (11.0)
		addRule("(?i)\\bCT\\b", Label.forCode("CT"), 0.92);	// word_CT_binarized = 1: CT (47.0/4.0)
		addRule("(?i)\\bMRI\\b", Label.forCode("MR"), 0.86);	// word_MRI_binarized = 1: MR (12.0/2.0)
		addRule("(?i)\\bEndoscopic\\b", Label.forCode("EN"), 0.86);	// word_Endoscopic_binarized = 1: EN (6.0/1.0)
		
		if (writer != null) {
			csv = new CSVWriter(writer, separator, quote);

			csvRow = new String[1 + rules.size()];
			if (!omitHeader) {
			    writeHeader();
			}
		}
		else {
		    csv = null;
		}
	}

	private void writeHeader() {
	    	if (csv == null)
	    	    throw new UnsupportedOperationException("no CSVWriter available");
	    	
		// write csv header
		csvRow[0] = "id";
		for (Prediction p : rules.values())
			csvRow[p.getId()+1] = p.getComment();
		csv.writeNext(csvRow);
	}

	/**
	 * Helper method.
	 */
	private void addRule(String regex, Label label, double confidence) {
		Prediction p = new Prediction(label, (float) confidence, rules.size());
		p.setComment("regex_" + regex);
		rules.put(Pattern.compile(regex).matcher(""), p);
	}

	@Override
	protected void predict(PMCArticle article, Figure fig, Graphic graphic) {
		for (Prediction p : predict(fig.id, fig.caption))
			overridePrediction(graphic, p.label, p.confidence);
	}

	/**
	 * Apply each rule.
	 * 
	 * @param caption
	 * @return
	 */
	protected List<Prediction> predict(String id, String caption) {
		List<Prediction> predictions = new ArrayList<Prediction>();
		
		System.err.println("PREDICTING: '" + id +"', '" + caption +"'");
		// init csv row to zeros
		Arrays.fill(csvRow, "0");
		csvRow[0] = id;
		
		for (Entry<Matcher, Prediction> rule : rules.entrySet()) {
			if (rule.getKey().reset(caption).find())
			{
				predictions.add(rule.getValue());
				csvRow[1+rule.getValue().getId()] = "1";
			}
		}
		if (predictions.isEmpty() && caption.length() > 5) {
			System.err.println("UNKNOWN: " + caption);
		}
		if (csv != null)
		    	csv.writeNext(csvRow);
		return predictions;
	}
	protected Vector match(String caption) {
	    	double[] matches = new double[rules.size()];
		for (Entry<Matcher, Prediction> rule : rules.entrySet()) {
			if (rule.getKey().reset(caption).find())
				matches[rule.getValue().getId()] = 1;
		}
		return new DenseVector(matches);
	}
	
	
	public static class XmlTest {
		public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
			String file = args[0];
			String uri = "file:" + new File(file).getAbsolutePath(); 
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(true);
			
			SAXParser parser = spf.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			
			RegexPredictor predictor = new RegexPredictor();
			XMLFilter filter = new PredictorXmlFilter(predictor, FileUtils.getBufferedFileWriter(new File(file + ".out.xml"), Charset.forName("UTF-8")));
			filter.setParent(reader);
						
			InputSource source = new InputSource(uri);
			filter.parse(source);
			predictor.destroy();
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.destroy();
		super.finalize();
	}
	
	public void destroy() {
		try {
			csv.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Test {
		static RegexPredictor predictor;
		
		static {
		    try {
			predictor = new RegexPredictor();
		    } catch (IOException e) {
			throw new RuntimeException(e);
		    }
		}

		public static void main(String[] args) throws SAXException,
				IOException, XPathExpressionException {
			Document doc = XmlParser.newDocumentBuilder().parse(
					new File(args[0]));
			XPathExpression articleExpr = XmlParser.compileXPath("//article");
			XPathExpression figExpr = XmlParser.compileXPath("./figure");
			XPathExpression captionExpr = XmlParser.compileXPath("./caption");
			XPathExpression graphicExpr = XmlParser.compileXPath("./graphic");
			
			for (Element articleElement : XmlParser.evaluateToElementArray(
					articleExpr, doc.getDocumentElement())) {

				for (Element figElement : XmlParser.evaluateToElementArray(
						figExpr, articleElement)) {
					
					String caption = XmlParser.evaluateToString(captionExpr, figElement);
					System.err.println("Testing '" + caption + "' ...");

					List<Prediction> predictions = predictor.predict(figElement.getAttribute("id"), caption);
					for (Prediction p : predictions) {
						System.out.println("CAP\t" + p.label + "\t"
								+ p.confidence + "\t" + caption);
						for (Element graphicElement : XmlParser
								.evaluateToElementArray(graphicExpr, figElement))
							System.out.println("GRAPHIC\t" + p.label + "\t"
									+ p.confidence + "\t"
									+ articleElement.getAttribute("dir") + "/"
									+ graphicElement.getAttribute("id"));
					}
					if (predictions.isEmpty())
						System.out.println("???\t" + caption);
				}
			}
		}
	}
	public static class StreamingMapper {		

		public static void main(String[] args) {
		    try {
			Writer writer = new OutputStreamWriter(System.out);
			RegexPredictor predictor = new RegexPredictor(writer, true, '\t', CSVWriter.NO_QUOTE_CHARACTER);
		    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		    	String line;
			while ((line = br.readLine()) != null) {
        			final String [] fields = line.split("\t");
			    	System.err.println("LINE ("+fields.length+"):\t'" + line +"'");
        			
        			// skip empty lines
        			if (line.length() == 0)
        			    continue;
        			
        			String id = fields[0];
        			String caption = fields[1];
        			System.err.println("Testing '" + caption + "' ...");
        
        			/* List<Prediction> predictions = */ predictor.predict(id, caption);
			}
			writer.close();
			System.exit(0);
		    } catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		    }
		}
	}
	
	
	public static class Mapper implements org.apache.hadoop.mapred.Mapper<Text, Text, Text, VectorWritable> {
	    
	    final RegexPredictor predictor;
	    
	    public Mapper() throws IOException {
		this.predictor = new RegexPredictor(null);
	    }

	    @Override
	    public void map(Text key, Text value,
		    OutputCollector<Text, VectorWritable> output,
		    Reporter reporter) throws IOException {
		final Vector matches = predictor.match(value.toString());
		output.collect(key, new VectorWritable(matches));
	    }

	    @Override
	    public void configure(JobConf job) {
	    }

	    @Override
	    public void close() throws IOException {
	    }
	    
	    /**
	     * Run a regex job.
	     * @param args
	     * @throws Exception
	     */
	    public static void main(String[] args) throws Exception {
		
		if (args.length != 2)
		{
		    System.err.println("Usage: \n java ...  <input_dir> <output_dir>");
		}
		
		JobConf conf = new JobConf();
		conf.setJobName("regextest");
		
		
		conf.setInputFormat(ImageInputFormat.class);
		conf.set(ImageInputFormat.FILES_PER_MAP, String.valueOf(100));
			
		conf.setMapperClass(RegexPredictor.Mapper.class);	
			
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(VectorWritable.class);

		//conf.setReducerClass(IdentityReducer.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(VectorWritable.class);

		conf.setOutputFormat(ImageOutputWriter.class);
		
		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		
		JobClient.runJob(conf);		
	    }
	}
}
