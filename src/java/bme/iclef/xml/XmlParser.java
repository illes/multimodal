package bme.iclef.xml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import bme.iclef.representation.PMCArticle;

public abstract class XmlParser {
	
	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	static {
		try  {
			dbf.setValidating(false);
			dbf.setNamespaceAware(false); // disable namespaces
			dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",	false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static XPathFactory xpf = XPathFactory.newInstance();
	private static XPath xpath = xpf.newXPath();
	public static XPathExpression compileXPath(String expr) throws XPathExpressionException
	{
		return xpath.compile(expr);		
	}

	
	
	public static DocumentBuilder newDocumentBuilder()
	{
		try {
			return dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	abstract List<PMCArticle> parse(File pmcXml) throws SAXException,
			IOException;

	public static Integer evaluateToInteger(XPathExpression expr, Element el)
			throws XPathExpressionException {
		try {
			return Integer.valueOf(evaluateToString(expr, el));
		} catch (NumberFormatException e) {
			// do nothing, will return null
		}
		return null;
	}

	public static String evaluateToString(XPathExpression expr, Element el)
			throws XPathExpressionException {
		return (String) expr.evaluate(el, XPathConstants.STRING);
	}

	public static NodeList evaluateToNodeList(XPathExpression expr, Element el)
			throws XPathExpressionException {
		return (NodeList) expr.evaluate(el, XPathConstants.NODESET);
	}

	public static StringBuilder evaluateToAppend(XPathExpression expr,
			Element el, StringBuilder sb, boolean clear)
			throws XPathExpressionException {
		if (clear)
			sb.setLength(0);
		NodeList textNodes = (NodeList) expr.evaluate(el,
				XPathConstants.NODESET);
		for (int i = 1; i < textNodes.getLength(); i++) {
			sb.append(((Text) textNodes.item(i)).getData());
		}
		return sb;
	}
	
	public static Element[] evaluateToElementArray(XPathExpression expr, Element el) throws XPathExpressionException
	{
		NodeList nodes = evaluateToNodeList(expr, el);
		Element[] ret = new Element[nodes.getLength()];
		for (int i = 0; i < nodes.getLength(); i++)
			ret[i] = (Element) nodes.item(i);
		return ret;
	}

}