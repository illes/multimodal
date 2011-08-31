package bme.iclef.predict;

import java.io.IOException;
import java.io.Writer;

import org.xml.sax.SAXException;

import bme.iclef.representation.PMCArticle;
import bme.iclef.xml.XmlFilter;

public class PredictorXmlFilter extends XmlFilter {


	final Predictor predictor;
	final Writer output;
	public PredictorXmlFilter(Predictor predictor, Writer output) {
		this.predictor = predictor;
		this.output = output;
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
		predictor.predict(article);
		try {
			output.write(article.toXML().toString(2));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

}
