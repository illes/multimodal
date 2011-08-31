/**
 * 
 */
package bme.iclef.xml;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import net.sf.practicalxml.builder.ElementNode;
import net.sf.practicalxml.builder.XmlBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;
import bme.iclef.utils.FileUtils;

/**
 * @author illes
 */
public class PMCParser extends XmlParser {
	/**
	 * <code>
	<article pmc="" pmid="" publisher-id="" doi="" dir="dd/d8" id="Am_J_Clin_Nutr_2010_May_24_91(5)_1180-1184">
	  <float id ="">
	    <graphics id="">
	     <prediction-set id="MODALITY">
	        <prediction id="X-RAY" confidence="1.0"/>
	     </prediction-set>
	    </graphics>
	    <description></description>
	    <reference></reference>
	    <reference></reference>
	  </float>
	</article>
	</code>
	 */
	private DocumentBuilder dBuilder;
	private XPathExpression pmcidExpr;
	private XPathExpression journalExpr;
	private XPathExpression articleIdExpr;
	private XPathExpression doiExpr;
	private XPathExpression publisherExpr;
	private XPathExpression figExpr;
	private XPathExpression figGraphicsExpr;
	private XPathExpression figLabelExpr;
	private XPathExpression figIdExpr;
	private XPathExpression graphicHrefExpr;
	private XPathExpression figCaptionExpr;
	private XPathExpression pmidExpr;
	private XPathExpression titleExpr;
	private XPathExpression subjectExpr;

	public PMCParser() throws XPathExpressionException {
		dBuilder = super.newDocumentBuilder();
		
		pmcidExpr = compileXPath("front/article-meta/article-id[@pub-id-type='pmc']/text()");
		pmidExpr = compileXPath("front/article-meta/article-id[@pub-id-type='pmid']/text()");
		doiExpr = compileXPath("front/article-meta/article-id[@pub-id-type='doi']/text()");
		articleIdExpr = compileXPath("@id"); // inside <article>
		titleExpr = compileXPath("front/article-meta/title-group/article-title/text()");
		subjectExpr = compileXPath("front/article-meta/article-categories/subj-group[@subj-group-type='heading']/subject/text()");
		journalExpr = compileXPath("front/journal-meta/journal-id[@journal-id-type='nlm-ta']/text()");
		publisherExpr = compileXPath("front/journal-meta/publisher/publisher-name/text()");
		figExpr = compileXPath("body//fig");
		figGraphicsExpr = compileXPath(".//graphic"); // inside <fig>
		figLabelExpr = compileXPath("label/text()"); // inside <fig>
		figCaptionExpr = compileXPath("caption//text()"); // inside <fig>
		figIdExpr = compileXPath("@id"); // inside <fig>
		graphicHrefExpr = compileXPath("@href"); // inside <graphic>
	}

	private StringBuilder buffer = new StringBuilder();

	@Override
	public List<PMCArticle> parse(File pmcXml) throws SAXException, IOException {
		try { 
			Document doc = dBuilder.parse(pmcXml);
			doc.getDocumentElement().normalize();
	
			NodeList nList = doc.getElementsByTagName("article");	
			List<PMCArticle> articles = new ArrayList<PMCArticle>(nList.getLength());
			for (int i = 0; i < nList.getLength(); i++) {
				Element articleElement = (Element) nList.item(i);
				
				// init
				PMCArticle article = parseArticle(articleElement);
				article.storeDir = pmcXml.getParent();	
				articles.add(article);
			}
			return articles;
		} catch (NumberFormatException e) {
			throw new SAXException("wrong number", e);
		} catch (XPathExpressionException e) {
			throw new SAXException("bad xpath", e);
		}
	}
	
	private PMCArticle parseArticle(Element articleElement) throws XPathExpressionException {
		PMCArticle article = new PMCArticle();
		article.pmcid = evaluateToInteger(pmcidExpr, articleElement);
		article.pmid = evaluateToInteger(pmidExpr, articleElement);
		article.id = evaluateToString(articleIdExpr, articleElement);
		article.title = evaluateToString(titleExpr, articleElement);
		article.subject = evaluateToString(subjectExpr, articleElement);
		article.journal = evaluateToString(journalExpr, articleElement);
		article.doi = evaluateToString(doiExpr, articleElement);
		article.publisher = evaluateToString(publisherExpr, articleElement);

		// iterate figures
		for (Element figureElement : evaluateToElementArray(figExpr, articleElement)) {
			Figure figure = parseFigure(figureElement);
			article.figureList.add(figure);
		}
		return article;
	}


	private Figure parseFigure(Element figureElement) throws XPathExpressionException {
		Figure figure = new Figure();
		figure.id = evaluateToString(figIdExpr, figureElement);
		figure.label = evaluateToString(figLabelExpr, figureElement).trim();
		figure.caption = evaluateToAppend(figCaptionExpr, figureElement, buffer, true).toString().trim();

		// iterate graphics
		for (Element graphicElement : evaluateToElementArray(figGraphicsExpr, figureElement)) {

			// init
			Graphic graphic = new Graphic();
			graphic.id = evaluateToString(graphicHrefExpr, graphicElement);

			// append
			figure.graphicList.add(graphic);
		}
		return figure;
	}


	public static void main(String args[]) {

		try {
			
			final List<PMCArticle> articles = new ArrayList<PMCArticle>();
			
			// parse files
			{
				PMCParser parser = new PMCParser();
				
				// read file names from stdin
				BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
				String xmlFile;
				while ((xmlFile = r.readLine()) != null)
				{
					System.err.println("Processing '" + xmlFile +"' ...");
					articles.addAll(parser.parse(new File(xmlFile)));
				}
			}
			
			// print XML
			{
				ElementNode xml = XmlBuilder.element("articles");
				for (PMCArticle article :articles)
					xml.addChild(article.toXML());
				FileUtils.writeToFile(xml.toString(2), new File(args[0]), Charset.forName("UTF-8"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
}
