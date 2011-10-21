package bme.iclef.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.mahout.math.VectorWritable;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.converters.AbstractFileLoader;

public class SequenceFileLoader extends AbstractFileLoader {

    /** the file extension. */
    public static String FILE_EXTENSION = ".seq";

    /** The reader for the data. */
    protected transient BufferedReader m_sourceReader;

    /** The placeholder for missing values. */
    protected double m_MissingValue = Double.NaN;

    protected SequenceFile.Reader m_sequenceFileReader;

    /**
     * default constructor.
     */
    public SequenceFileLoader() {
	// No instances retrieved yet
	setRetrieval(NONE);
    }

    @Override
    public Instances getDataSet() throws IOException {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Instance getNextInstance(Instances structure) throws IOException {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public String getFileDescription() {
	return "Apache Mahout sequence file";
    }

    @Override
    public String getFileExtension() {
	return FILE_EXTENSION;
    }

    @Override
    public String[] getFileExtensions() {
	return new String[] { getFileExtension() };
    }

    /**
     * Resets the Loader object and sets the source of the data set to be the
     * supplied Stream object.
     * 
     * @param input
     *            the input stream
     * @exception IOException
     *                if an error occurs
     */
    @Override
    public void setSource(InputStream input) throws IOException {
	throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Resets the {@link SequenceFileLoader} object and sets the source of the
     * data set to be the supplied {@link File} object.
     * 
     * @param file
     *            the source file.
     * @exception IOException
     *                if an error occurs
     */
    @Override
    public void setSource(File file) throws IOException {
	super.setSource(file);
    }

    /**
     * Determines and returns (if possible) the structure (internally the
     * header) of the data set as an empty set of instances.
     * 
     * @return the structure of the data set as an empty set of Instances
     * @exception IOException
     *                if an error occurs
     */
    @Override
    public Instances getStructure() throws IOException {
	if ((m_sourceFile == null) && (m_sourceReader == null)) {
	    throw new IllegalStateException("No source has been specified");
	}

	if (m_structure == null) {
	    Configuration config = new Configuration();
	    Path path = new Path(m_sourceFile.getPath());
	    m_sequenceFileReader = new SequenceFile.Reader(FileSystem
		    .get(config), path, config);
	    readStructure(m_sequenceFileReader);
	}

	return m_structure;
    }

    private void readStructure(SequenceFile.Reader reader)
	    throws IOException {
	try {
	    Text key = (Text) reader.getKeyClass().newInstance();
	    VectorWritable value = (VectorWritable) reader.getValueClass()
		    .newInstance();
	    if (reader.next(key, value)) {
		
		final int vectorSize = value.get().size();
		final FastVector attribNames = new FastVector(vectorSize+2);
		
		attribNames.addElement(new Attribute("id")); // TODO make configurable
		for (int i = 0; i < vectorSize; i++)
		    attribNames.addElement(new Attribute("attr_" + i));		
		attribNames.addElement(new Attribute("label")); // TODO make configurable
		
		final String relationName;
		if (m_sourceFile != null)
			relationName = (m_sourceFile.getName()).replace(
					"(?i)" + Pattern.quote(getFileExtension()) + "$", "");
		else
			relationName = "stream";
		m_structure = new Instances(relationName, attribNames, 0);
	    } else {
		throw new IllegalStateException("Empty sequence file.");
	    }
	} catch (InstantiationException e) {
	    throw new RuntimeException(e);
	} catch (IllegalAccessException e) {
	    throw new RuntimeException(e);
	} finally {
	    reader.close();
	}
    }

    @Override
    public String getRevision() {
	return "0.1";
    }

}
