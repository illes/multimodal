package bme.iclef.evaluate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import bme.iclef.predict.Prediction.Group;
import bme.iclef.predict.Prediction.Label;
import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;
import bme.iclef.xml.XmlFilter;

public class EvaluateXmlFilter extends XmlFilter {

	final ConfusionMatrix labelCM;
	final ConfusionMatrix groupCM;
	
	final Map<String, Label> groundTruth;
	
	final Set<String> seen = new HashSet<String>();
	
	public static boolean useDefaultForMissing = true; // TODO set to true for submission
	
	final static String NONE = "NONE";
	
	public EvaluateXmlFilter(Map<String, Label> groundTruth) {
		this.labelCM = new ConfusionMatrix(Label.codeSet(), NONE);
		this.groupCM = new ConfusionMatrix(Group.codeSet(), NONE);
		this.groundTruth = groundTruth;
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}
	
	@Override
	public void endDocument() throws SAXException {
		System.err.println("Seen " + seen.size() + " graphics");
		System.err.println("Label statistics: ");
		System.err.println(labelCM.summarize());
		System.err.println("Group statistics: ");
		System.err.println(groupCM.summarize());
	}
	
	@Override
	protected void consume(PMCArticle article) {
		for (Figure f : article.figureList)
			for (Graphic g : f.graphicList)
				evaluate(g, f.caption);
	}

	private void evaluate(Graphic g, Object clue) {
		if (seen.contains(g.id))
		{
			//throw new IllegalStateException
			System.err.println("ERROR: Duplicate graphic: " + g.id);
			return;
		}
		if (!groundTruth.containsKey(g.id))
		{
			System.err.println("WARNING: No ground truth  for " + g.id + ", skipping...");
			return;
		}
		final Label gt = groundTruth.get(g.id);
		seen.add(g.id);
		// TODO consider probability estimates
		// pick max
		if (g.predictionMap.isEmpty())
			evaluate(gt, useDefaultForMissing ? Label.getDefault() : null, clue);
		else
		{
			double max = Double.NEGATIVE_INFINITY;
			Label bestLabel = null;
			for (Entry<Label, Float> l : g.predictionMap.entrySet())
			{
				if (max < l.getValue())
				{
					max = l.getValue();
					bestLabel = l.getKey();
				}
			}
			evaluate(gt, bestLabel, clue);
		}		
	}

	private void evaluate(Label gt, Label pred, Object clue) {
		if (pred == null)
		{
			labelCM.increment(gt.code, NONE);
			groupCM.increment(gt.group.name(), NONE);
			System.err.println("LMISS\t" + gt.code +"\t" + NONE + "\t" + String.valueOf(clue));					
			System.err.println("GMISS\t" + gt.group.name() +"\t" + NONE + "\t" + String.valueOf(clue));
		}
		else
		{
			if (pred.code != null)
			{
				labelCM.increment(gt.code, pred.code);
				if (gt != pred)
				{
					System.err.println("LMISS\t" + gt.code +"\t" + pred.code + "\t" + String.valueOf(clue));					
				}
			}
			else
				labelCM.increment(gt.code, NONE);
			
			groupCM.increment(gt.group.name(), pred.group.name());
			if (gt.group != pred.group)
			{
				System.err.println("GMISS\t" + gt.group.name() +"\t" + pred.group.name() + "\t" + String.valueOf(clue));
			}
		}
	}
	
	
	public static Map<String, Label> groundTruthFromFile(String file) throws IOException
	{
		Map<String, Label> gt = new HashMap<String, Label>();
		BufferedReader r = new BufferedReader(new FileReader(new File(file)));
		String line;
		Pattern delimiter = Pattern.compile(".jpg, ");
		while ((line = r.readLine()) != null)
		{
			String[] fields = delimiter.split(line);
			if (fields.length != 2 )
				throw new IllegalArgumentException("Unrecognozed line: '" + line +"'");
			final Label l = Label.forCode(fields[1].trim());
			if (l == null)
				throw new IllegalArgumentException("Unrecognized label '" + fields[1] +"'");
			gt.put(fields[0], l);
		}
		return gt;
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
			
			XMLFilter filter = new EvaluateXmlFilter(EvaluateXmlFilter.groundTruthFromFile(gtFile));
			filter.setParent(reader);
						
			InputSource source = new InputSource(uri);
			filter.parse(source);
		}
	}
	
	
	
	
}
