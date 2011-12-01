package bme.iclef.hadoop;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import bme.iclef.predict.RegexPredictor;
import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;
import bme.iclef.xml.XmlFilter;

/**
 * Simple class to write image identifiers and captions from an ImageCLEF style
 * XML file to a Hadoop {@link SequenceFile}. The output sequence file is
 * compatible with {@link RegexPredictor.Mapper}.
 * 
 * @author Illes Solt
 * 
 */
public class SequenceFileWriterXmlFilter extends XmlFilter {

    SequenceFile.Writer writer;
    Path path;

    /**
     * 
     * @param path - the output {@link SequenceFile}'s path
     */
    public SequenceFileWriterXmlFilter(Path path) {
	if (path == null)
	    throw new NullPointerException();
	this.path = path;
    }

    @Override
    public void startDocument() throws SAXException {
	try {
	    super.startDocument();
	    Configuration conf = new Configuration();
	    FileSystem fs = FileSystem.get(conf);
	    fs.setConf(conf);
	    writer = SequenceFile.createWriter(fs, conf, path, Text.class,
		    Text.class);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    @Override
    protected void consume(PMCArticle article) {
	Text key = new Text();
	Text value = new Text();
	try {
	    for (Figure f : article.figureList) {
		value.set(f.caption);
		for (Graphic g : f.graphicList) {
		    key.set(g.id);
		    writer.append(key, value);
		}
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    @Override
    public void endDocument() throws SAXException {
	try {
	    writer.close();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	} finally {
	    writer = null;
	    super.endDocument();
	}
    }

    /**
     * Test the {@link SequenceFileWriterXmlFilter} with a single XML file.
     */
    public static void main(String[] args) throws ParserConfigurationException,
	    SAXException, IOException {
	String file = args[0];
	String uri = "file:" + new File(file).getAbsolutePath();
	SAXParserFactory spf = SAXParserFactory.newInstance();
	spf.setNamespaceAware(true);
	spf.setValidating(true);

	SAXParser parser = spf.newSAXParser();
	XMLReader reader = parser.getXMLReader();

	XMLFilter filter = new SequenceFileWriterXmlFilter(new Path(file
		+ ".captions.seq"));
	filter.setParent(reader);

	InputSource source = new InputSource(uri);
	filter.parse(source);
    }
}
