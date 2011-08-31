/**
 * 
 */
package bme.iclef.representation;

import static net.sf.practicalxml.builder.XmlBuilder.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.practicalxml.builder.ElementNode;
import net.sf.practicalxml.builder.Node;
import net.sf.practicalxml.builder.XmlBuilder;
import bme.iclef.predict.Prediction.Group;
import bme.iclef.predict.Prediction.Label;

public class PMCArticle {
	public Integer pmcid;
	public Integer pmid;
	public String publisher;
	public String journal;
	public String doi;
	public String id;
	public String storeDir;
	public String title;
	public String subject;
	public final List<Figure> figureList = new ArrayList<PMCArticle.Figure>();
	
	public ElementNode toXML() {
		final ElementNode articleNode = 
			element("article",
					attribute("pmc", pmcid),
					attribute("pmid", pmcid),
					attribute("publisher", publisher), 
					attribute("doi", doi), 
					attribute("journal", journal), 
					attribute("dir", storeDir),
					attribute("title", title),
					attribute("id", id),
					attribute("subject", subject)
					);

		for (Figure f : figureList)
			articleNode.addChild(f.toXML());

		return articleNode;
	}

	public static class Graphic {
		public String id;
		public final Map<Label, Float> predictionMap = new HashMap<Label, Float>();
		
		ElementNode toXML() {
			final ElementNode node = 
				element("graphic", 
					attribute("id", id));
			for(Group g : Group.values())
			{
				final ElementNode predictionSetNode = element("prediction-set", attribute("group", g.name())); 
				for (Entry<Label, Float> e : predictionMap.entrySet())
					if (e.getKey().group == g)
						predictionSetNode.addChild(
								element("prediction",
										attribute("label", e.getKey().code),
										attribute("confidence", e.getValue())));
				node.addChild(predictionSetNode);
			}
			return node;
		}

	}

	public static class Figure {
		public String id;
		public String label;
		public final List<Graphic> graphicList = new ArrayList<Graphic>();
		public String caption;
		public final List<String> references = new ArrayList<String>();

		ElementNode toXML() {
			final ElementNode node = 
				element("figure",
					attribute("id", id), 
					attribute("label", label),
					attribute("caption", caption));

			for (String r : references)
				element("reference", attribute("text", r));

			for (PMCArticle.Graphic g : graphicList)
				node.addChild(g.toXML());

			return node;
		}
	}

//	@SuppressWarnings("unused")
//	private static Node attribute(String key, boolean value)
//			throws NullPointerException {
//		return attribute(key, String.valueOf(value));
//	}
//
//	@SuppressWarnings("unused")
//	private static Node attribute(String key, int value)
//			throws NullPointerException {
//		return attribute(key, String.valueOf(value));
//	}
//
//	@SuppressWarnings("unused")
//	private static Node attribute(String key, short value)
//			throws NullPointerException {
//		return attribute(key, String.valueOf(value));
//	}
//
//	@SuppressWarnings("unused")
//	private static Node attribute(String key, float value)
//			throws NullPointerException {
//		return attribute(key, String.valueOf(value));
//	}
//
//	@SuppressWarnings("unused")
//	private static Node attribute(String key, double value)
//			throws NullPointerException {
//		return attribute(key, String.valueOf(value));
//	}
	
	/**
	 * Helper method. Returns <code>null</code> on <code>null</code> value resulting in a missing attribute (instead of an empty one). 
	 */
	static Node attribute(String key, Object value)
		throws NullPointerException {
		if (key == null)
			throw new NullPointerException();
		if (value != null)
			return XmlBuilder.attribute(key, String.valueOf(value));
		return null;
	}

}