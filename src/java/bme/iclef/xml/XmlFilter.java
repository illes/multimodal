package bme.iclef.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import bme.iclef.predict.Prediction.Label;
import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;

public abstract class XmlFilter extends XMLFilterImpl {
	
	protected boolean echo = true;
	protected PMCArticle article;
	protected Figure figure;
	protected Graphic graphic;
	
	static Integer toInteger(String str)
	{
		return (str == null || str.isEmpty()) ? null : Integer.valueOf(str);
	}
	
	static Float toFloat(String str)
	{
		return str == null ? null : Float.valueOf(str);
	}
	
	void clear()
	{
		article = null;
		figure = null;
		graphic = null;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (eitherEquals("article", localName, qName))
		{
			clear();
			article = new PMCArticle();
			article.doi = atts.getValue("doi");
			article.pmid = toInteger(atts.getValue("pmid"));
			article.pmcid = toInteger(atts.getValue("pmcid"));
			article.storeDir = atts.getValue("dir");
			article.journal = atts.getValue("journal");
			article.title = atts.getValue("title");
			article.publisher = atts.getValue("publisher");
			article.subject = atts.getValue("subject");
		}
		else if (eitherEquals("figure", localName, qName))
		{
			figure = new Figure();
			figure.id = atts.getValue("id");
			figure.caption = atts.getValue("caption");
			figure.label = atts.getValue("label");
			// store
			article.figureList.add(figure);
		}
		else if (eitherEquals("graphic", localName, qName))
		{
			
			graphic = new Graphic();
			graphic.id = atts.getValue("id");
			// store
			figure.graphicList.add(graphic);
		}
		else if (eitherEquals("reference", localName, qName))
		{
			// store
			figure.references.add(atts.getValue("text"));
		}
		else if (eitherEquals("prediction", localName, qName))
		{
			graphic.predictionMap.put(Label.forCode(atts.getValue("label")), toFloat(atts.getValue("confidence")));
		}
		else if (eitherEquals("reference", localName, qName))
		{
			figure.references.add(atts.getValue("text"));
		}

		if (echo)
			super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (eitherEquals("article", localName, qName))
		{
			consume(article);
			clear();
		}
		else if (eitherEquals("figure", localName, qName))
		{
			figure = null;
		}
		else if (eitherEquals("graphic", localName, qName))
		{
			
			graphic = null;
		}
		
		if (echo)
			super.endElement(uri, localName, qName);
	}
	
	protected abstract void consume(PMCArticle article);

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (echo)
			super.characters(ch, start, length);
	}
	
	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		if (echo)
			super.processingInstruction(target, data);
	}

	protected static boolean eitherEquals(String tagName, String localName, String qName)	{
		return (tagName.equals(localName) || tagName.equals(qName));
	}
}
