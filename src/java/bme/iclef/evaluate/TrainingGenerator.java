package bme.iclef.evaluate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import bme.iclef.predict.Prediction.Label;
import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;
import bme.iclef.utils.FileUtils;
import bme.iclef.xml.XmlFilter;

public class TrainingGenerator extends XmlFilter {

	private Map<String, Label> groundTruth;
	
	private final File labelOutputDir;
	private final File groupOutputDir;
	private final Writer output;
	

	private final static Charset charset = Charset.forName("UTF-8"); 

	public TrainingGenerator(Map<String, Label> groundTruth, File outputDir) throws FileNotFoundException {
		this.groundTruth = groundTruth;
		this.labelOutputDir = new File(outputDir, "label");
		this.groupOutputDir = new File(outputDir, "group");
		this.output = FileUtils.getBufferedFileWriter(new File(outputDir, "articles.xml"), charset);
		
		// create directories
		outputDir.mkdir();
		this.labelOutputDir.mkdir();
		this.groupOutputDir.mkdir();
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		try {
			output.write("<articles>\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
	
	@Override
	public void endDocument() throws SAXException {
		try {
			output.write("</articles>\n");
			output.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		super.endDocument();
	}
	
	@Override
	protected void consume(PMCArticle article) {
		boolean hasGroundTruth = false;;
		
		for (Figure f : article.figureList)
		{
			for (Graphic g : f.graphicList)
			{
				final Label gt = groundTruth.get(g.id);
				if (gt == null)
					continue;
				hasGroundTruth = true;
				try {
					writeExample(g.id, f.caption, gt);
				} catch (IOException e) {
					throw new RuntimeException(e);				
				}
			}
		}
		if (hasGroundTruth)
		{
			try {
				output.write(article.toXML().toString(2));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	}

	
	
	private void writeExample(String id, String caption, Label gt) throws IOException {
		// per label
		write(labelOutputDir, id, gt.code, caption);
		
		// per group
		write(groupOutputDir, id, gt.group.name(), caption);
	}
	
	private static void write(File dir, String id, String className, String content) throws IOException
	{
		File outDir = new File(dir, className);
		outDir.mkdir();
		FileUtils.writeToFile(content, new File(outDir, id + ".txt"), charset);
	}



	public static class XmlTest {
		public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
			String xmlFile = args[0];
			String gtFile = args[1];
			String uri = "file:" + new File(xmlFile).getAbsolutePath(); 
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(true);
			
			SAXParser parser = spf.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			
			XMLFilter filter = new TrainingGenerator(EvaluateXmlFilter.groundTruthFromFile(gtFile), new File("gt"));
			filter.setParent(reader);
						
			InputSource source = new InputSource(uri);
			filter.parse(source);
		}
	}
	
	
	
	
}
