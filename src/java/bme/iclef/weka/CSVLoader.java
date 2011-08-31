/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    CSVLoader.java
 *    Copyright (C) 2000 University of Waikato, Hamilton, New Zealand
 *
 */

package bme.iclef.weka;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.converters.AbstractFileLoader;
import weka.core.converters.BatchConverter;
import weka.core.converters.ConverterUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * <!-- globalinfo-start --> Reads a source that is in comma separated or tab
 * separated format. Assumes that the first row in the file determines the
 * number of and names of the attributes.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -N &lt;range&gt;
 *  The range of attributes to force type to be NOMINAL.
 *  'first' and 'last' are accepted as well.
 *  Examples: "first-last", "1,4,5-27,50-last"
 *  (default: -none-)
 * </pre>
 * 
 * <pre>
 * -S &lt;range&gt;
 *  The range of attribute to force type to be STRING.
 *  'first' and 'last' are accepted as well.
 *  Examples: "first-last", "1,4,5-27,50-last"
 *  (default: -none-)
 * </pre>
 * 
 * <pre>
 * -M &lt;str&gt;
 *  The string representing a missing value.
 *  (default: ?)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 6002 $
 * @see Loader
 */
public class CSVLoader extends AbstractFileLoader implements BatchConverter,
		OptionHandler {

	/** for serialization. */
	static final long serialVersionUID = 5607529739745491340L;

	/** the file extension. */
	public static String FILE_EXTENSION = ".csv";

	/**
	 * A list of hash tables for accumulating nominal values during parsing.
	 */
	protected FastVector m_cumulativeStructure;

	/**
	 * Holds instances accumulated so far.
	 */
	protected FastVector m_cumulativeInstances;

	/** The reader for the data. */
	protected transient BufferedReader m_sourceReader;

	/** Tokenizer for the data. */
	protected transient StreamTokenizer m_st;

	/** The range of attributes to force to type nominal. */
	protected Range m_NominalAttributes = new Range();

	/** The range of attributes to force to type string. */
	protected Range m_StringAttributes = new Range();

	/** The placeholder for missing values. */
	protected String m_MissingValue = "?";

	/** whether the first row has been read. */
	protected boolean m_FirstCheck;

	/**
	 * default constructor.
	 */
	public CSVLoader() {
		// No instances retrieved yet
		setRetrieval(NONE);
	}

	/**
	 * Get the file extension used for arff files.
	 * 
	 * @return the file extension
	 */
	public String getFileExtension() {
		return FILE_EXTENSION;
	}

	/**
	 * Returns a description of the file type.
	 * 
	 * @return a short file description
	 */
	public String getFileDescription() {
		return "CSV data files";
	}

	/**
	 * Gets all the file extensions used for this type of file.
	 * 
	 * @return the file extensions
	 */
	public String[] getFileExtensions() {
		return new String[] { getFileExtension() };
	}

	/**
	 * Returns a string describing this attribute evaluator.
	 * 
	 * @return a description of the evaluator suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Reads a source that is in comma separated or tab separated format. "
				+ "Assumes that the first row in the file determines the number of "
				+ "and names of the attributes.";
	}

	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * @return an enumeration of all the available options.
	 */
	public Enumeration<Option> listOptions() {
		Vector<Option> result = new Vector<Option>();

		result.addElement(new Option(
				"\tThe range of attributes to force type to be NOMINAL.\n"
						+ "\t'first' and 'last' are accepted as well.\n"
						+ "\tExamples: \"first-last\", \"1,4,5-27,50-last\"\n"
						+ "\t(default: -none-)", "N", 1, "-N <range>"));

		result.addElement(new Option(
				"\tThe range of attribute to force type to be STRING.\n"
						+ "\t'first' and 'last' are accepted as well.\n"
						+ "\tExamples: \"first-last\", \"1,4,5-27,50-last\"\n"
						+ "\t(default: -none-)", "S", 1, "-S <range>"));

		result.addElement(new Option(
				"\tThe string representing a missing value.\n"
						+ "\t(default: ?)", "M", 1, "-M <str>"));

		return result.elements();
	}

	/**
	 * Parses a given list of options.
	 * <p/>
	 * 
	 * <!-- options-start --> Valid options are:
	 * <p/>
	 * 
	 * <pre>
	 * -N &lt;range&gt;
	 *  The range of attributes to force type to be NOMINAL.
	 *  'first' and 'last' are accepted as well.
	 *  Examples: "first-last", "1,4,5-27,50-last"
	 *  (default: -none-)
	 * </pre>
	 * 
	 * <pre>
	 * -S &lt;range&gt;
	 *  The range of attribute to force type to be STRING.
	 *  'first' and 'last' are accepted as well.
	 *  Examples: "first-last", "1,4,5-27,50-last"
	 *  (default: -none-)
	 * </pre>
	 * 
	 * <pre>
	 * -M &lt;str&gt;
	 *  The string representing a missing value.
	 *  (default: ?)
	 * </pre>
	 * 
	 * <!-- options-end -->
	 * 
	 * @param options
	 *            the list of options as an array of strings
	 * @throws Exception
	 *             if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		String tmpStr;

		tmpStr = Utils.getOption('N', options);
		if (tmpStr.length() != 0)
			setNominalAttributes(tmpStr);
		else
			setNominalAttributes("");

		tmpStr = Utils.getOption('S', options);
		if (tmpStr.length() != 0)
			setStringAttributes(tmpStr);
		else
			setStringAttributes("");

		tmpStr = Utils.getOption('M', options);
		if (tmpStr.length() != 0)
			setMissingValue(tmpStr);
		else
			setMissingValue("?");
	}

	/**
	 * Gets the current settings of the Classifier.
	 * 
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		Vector<String> result;

		result = new Vector<String>();

		if (getNominalAttributes().length() > 0) {
			result.add("-N");
			result.add(getNominalAttributes());
		}

		if (getStringAttributes().length() > 0) {
			result.add("-S");
			result.add(getStringAttributes());
		}

		result.add("-M");
		result.add(getMissingValue());

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Sets the attribute range to be forced to type nominal.
	 * 
	 * @param value
	 *            the range
	 */
	public void setNominalAttributes(String value) {
		m_NominalAttributes.setRanges(value);
	}

	/**
	 * Returns the current attribute range to be forced to type nominal.
	 * 
	 * @return the range
	 */
	public String getNominalAttributes() {
		return m_NominalAttributes.getRanges();
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String nominalAttributesTipText() {
		return "The range of attributes to force to be of type NOMINAL, example "
				+ "ranges: 'first-last', '1,4,7-14,50-last'.";
	}

	/**
	 * Sets the attribute range to be forced to type string.
	 * 
	 * @param value
	 *            the range
	 */
	public void setStringAttributes(String value) {
		m_StringAttributes.setRanges(value);
	}

	/**
	 * Returns the current attribute range to be forced to type string.
	 * 
	 * @return the range
	 */
	public String getStringAttributes() {
		return m_StringAttributes.getRanges();
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String stringAttributesTipText() {
		return "The range of attributes to force to be of type STRING, example "
				+ "ranges: 'first-last', '1,4,7-14,50-last'.";
	}

	/**
	 * Sets the placeholder for missing values.
	 * 
	 * @param value
	 *            the placeholder
	 */
	public void setMissingValue(String value) {
		m_MissingValue = value;
	}

	/**
	 * Returns the current placeholder for missing values.
	 * 
	 * @return the placeholder
	 */
	public String getMissingValue() {
		return m_MissingValue;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String missingValueTipText() {
		return "The placeholder for missing values, default is '?'.";
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
		m_structure = null;
		m_sourceFile = null;
		m_File = null;
		m_FirstCheck = true;

		m_sourceReader = new BufferedReader(new InputStreamReader(input));
	}

	/**
	 * Resets the Loader object and sets the source of the data set to be the
	 * supplied File object.
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
			throw new IOException("No source has been specified");
		}

		if (m_structure == null) {
			try {
				m_st = new StreamTokenizer(m_sourceReader);
				initTokenizer(m_st);
				readStructure(m_st);
			} catch (FileNotFoundException ex) {
			}
		}

		return m_structure;
	}

	/**
	 * reads the structure.
	 * 
	 * @param st
	 *            the stream tokenizer to read from
	 * @throws IOException
	 *             if reading fails
	 */
	private void readStructure(StreamTokenizer st) throws IOException {
		readHeader(st);
	}

	/**
	 * Return the full data set. If the structure hasn't yet been determined by
	 * a call to getStructure then method should do so before processing the
	 * rest of the data set.
	 * 
	 * @return the structure of the data set as an empty set of Instances
	 * @exception IOException
	 *                if there is no source or parsing fails
	 */
	@Override
	public Instances getDataSet() throws IOException {
		if ((m_sourceFile == null) && (m_sourceReader == null)) {
			throw new IOException("No source has been specified");
		}

		if (m_structure == null) {
			getStructure();
		}

		if (m_st == null) {
			m_st = new StreamTokenizer(m_sourceReader);
			initTokenizer(m_st);
		}

		m_st.ordinaryChar(',');
		m_st.ordinaryChar('\t');

		m_cumulativeStructure = new FastVector(m_structure.numAttributes());
		for (int i = 0; i < m_structure.numAttributes(); i++) {
			m_cumulativeStructure.addElement(new Hashtable<Object, Integer>());
		}

		m_cumulativeInstances = new FastVector();
		FastVector current;
		while ((current = getInstance(m_st)) != null) {
			m_cumulativeInstances.addElement(current);
		}

		FastVector atts = new FastVector(m_structure.numAttributes());
		for (int i = 0; i < m_structure.numAttributes(); i++) {
			String attname = m_structure.attribute(i).name();
			Hashtable<?, ?> tempHash = ((Hashtable<?, ?>) m_cumulativeStructure
					.elementAt(i));
			if (tempHash.size() == 0) {
				atts.addElement(new Attribute(attname));
			} else {
				if (m_StringAttributes.isInRange(i)) {
					atts.addElement(new Attribute(attname, (FastVector) null));
				} else {
					FastVector values = new FastVector(tempHash.size());
					// add dummy objects in order to make the FastVector's size
					// == capacity
					for (int z = 0; z < tempHash.size(); z++) {
						values.addElement("dummy");
					}
					Enumeration<?> e = tempHash.keys();
					while (e.hasMoreElements()) {
						Object ob = e.nextElement();
						// if (ob instanceof Double) {
						int index = ((Integer) tempHash.get(ob)).intValue();
						String s = ob.toString();
						if (s.startsWith("'") || s.startsWith("\""))
							s = s.substring(1, s.length() - 1);
						values.setElementAt(new String(s), index);
						// }
					}
					atts.addElement(new Attribute(attname, values));
				}
			}
		}

		// make the instances
		String relationName;
		if (m_sourceFile != null)
			relationName = (m_sourceFile.getName()).replaceAll(
					"\\.[cC][sS][vV]$", "");
		else
			relationName = "stream";
		Instances dataSet = new Instances(relationName, atts,
				m_cumulativeInstances.size());

		for (int i = 0; i < m_cumulativeInstances.size(); i++) {
			current = ((FastVector) m_cumulativeInstances.elementAt(i));
			double[] vals = new double[dataSet.numAttributes()];
			for (int j = 0; j < current.size(); j++) {
				Object cval = current.elementAt(j);
				if (cval instanceof String) {
					if (((String) cval).compareTo(m_MissingValue) == 0) {
						vals[j] = Instance.missingValue();
					} else {
						if (dataSet.attribute(j).isString()) {
							vals[j] = dataSet.attribute(j).addStringValue(
									(String) cval);
						} else if (dataSet.attribute(j).isNominal()) {
							// find correct index
							Hashtable<Object, Integer> lookup = (Hashtable<Object, Integer>) m_cumulativeStructure
									.elementAt(j);
							int index = ((Integer) lookup.get(cval)).intValue();
							vals[j] = index;
						} else {
							throw new IllegalStateException(
									"Wrong attribute type at position "
											+ (i + 1) + "!!!");
						}
					}
				} else if (dataSet.attribute(j).isNominal()) {
					if (cval instanceof Double)
						cval = doubleToString((Double)cval);
					// find correct index
					Hashtable<Object, Integer> lookup = (Hashtable<Object, Integer>) m_cumulativeStructure
							.elementAt(j);
					int index = ((Integer) lookup.get(cval)).intValue();
					vals[j] = index;
				} else if (dataSet.attribute(j).isString()) {
					vals[j] = dataSet.attribute(j).addStringValue(doubleToString((Double)cval));
				} else {
					vals[j] = ((Double) cval).doubleValue();
				}
			}
			dataSet.add(new Instance(1.0, vals));
		}
		m_structure = new Instances(dataSet, 0);
		setRetrieval(BATCH);
		m_cumulativeStructure = null; // conserve memory

		// close the stream
		m_sourceReader.close();

		return dataSet;
	}

	/**
	 * CSVLoader is unable to process a data set incrementally.
	 * 
	 * @param structure
	 *            ignored
	 * @return never returns without throwing an exception
	 * @exception IOException
	 *                always. CSVLoader is unable to process a data set
	 *                incrementally.
	 */
	@Override
	public Instance getNextInstance(Instances structure) throws IOException {
		throw new IOException("CSVLoader can't read data sets incrementally.");
	}

	/**
	 * Attempts to parse a line of the data set.
	 * 
	 * @param tokenizer
	 *            the tokenizer
	 * @return a FastVector containg String and Double objects representing the
	 *         values of the instance.
	 * @exception IOException
	 *                if an error occurs
	 * 
	 *                <pre>
	 * <jml>
	 *    private_normal_behavior
	 *      requires: tokenizer != null;
	 *      ensures: \result  != null;
	 *  also
	 *    private_exceptional_behavior
	 *      requires: tokenizer == null
	 *                || (* unsucessful parse *);
	 *      signals: (IOException);
	 * </jml>
	 * </pre>
	 */
	private FastVector getInstance(StreamTokenizer tokenizer)
			throws IOException {

		FastVector current = new FastVector();

		// Check if end of file reached.
		ConverterUtils.getFirstToken(tokenizer);
		if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
			return null;
		}
		boolean first = true;
		boolean wasSep;

		while (tokenizer.ttype != StreamTokenizer.TT_EOL
				&& tokenizer.ttype != StreamTokenizer.TT_EOF) {

			// Get next token
			if (!first) {
				ConverterUtils.getToken(tokenizer);
			}

			if (tokenizer.ttype == ',' || tokenizer.ttype == '\t'
					|| tokenizer.ttype == StreamTokenizer.TT_EOL) {
				current.addElement(m_MissingValue);
				wasSep = true;
			} else {
				wasSep = false;
				if (tokenizer.sval.equals(m_MissingValue)) {
					current.addElement(new String(m_MissingValue));
				} else {
					// try to parse as a number
					try {
						double val = Double.parseDouble(tokenizer.sval + "d"); // HACK
																				// to
																				// make
																				// "3D"
																				// be
																				// parsed
																				// as
																				// "3D"
																				// instead
																				// of
																				// 3.0
						current.addElement(new Double(val));
					} catch (NumberFormatException e) {
						// otherwise assume its an enumerated value
						current.addElement(new String(tokenizer.sval));
					}
				}
			}

			if (!wasSep) {
				ConverterUtils.getToken(tokenizer);
			}
			first = false;
		}

		// check number of values read
		if (current.size() != m_structure.numAttributes()) {
			ConverterUtils.errms(tokenizer, "wrong number of values. Read "
					+ current.size() + ", expected "
					+ m_structure.numAttributes());
		}

		// check for structure update
		try {
			checkStructure(current);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return current;
	}

	/**
	 * Checks the current instance against what is known about the structure of
	 * the data set so far. If there is a nominal value for an attribute that
	 * was beleived to be numeric then all previously seen values for this
	 * attribute are stored in a Hashtable.
	 * 
	 * @param current
	 *            a <code>FastVector</code> value
	 * @exception Exception
	 *                if an error occurs
	 * 
	 *                <pre>
	 * <jml>
	 *    private_normal_behavior
	 *      requires: current != null;
	 *  also
	 *    private_exceptional_behavior
	 *      requires: current == null
	 *                || (* unrecognized object type in current *);
	 *      signals: (Exception);
	 * </jml>
	 * </pre>
	 */
	private void checkStructure(FastVector current) throws Exception {
		if (current == null) {
			throw new Exception("current shouldn't be null in checkStructure");
		}

		// initialize ranges, if necessary
		if (m_FirstCheck) {
			m_NominalAttributes.setUpper(current.size() - 1);
			m_StringAttributes.setUpper(current.size() - 1);
			m_FirstCheck = false;
		}

		for (int i = 0; i < current.size(); i++) {
			Object ob = current.elementAt(i);
			if ((ob instanceof String) || (m_NominalAttributes.isInRange(i)) || (m_StringAttributes.isInRange(i))) {
				if (ob instanceof Double)
					ob = doubleToString((Double)ob);
				
				if (ob.toString().compareTo(m_MissingValue) == 0) {
					// do nothing
				} else {
					Hashtable<Object, Integer> tempHash = (Hashtable<Object, Integer>) m_cumulativeStructure.elementAt(i);
					if (!tempHash.containsKey(ob)) {
						// may have found a nominal value in what was previously
						// thought to be a numeric variable.
						if (tempHash.size() == 0) {
							for (int j = 0; j < m_cumulativeInstances.size(); j++) {
								FastVector tempUpdate = ((FastVector) m_cumulativeInstances
										.elementAt(j));
								Object tempO = tempUpdate.elementAt(i);
								if (tempO instanceof String) {
									// must have been a missing value
								} else {
									tempO = doubleToString((Double)tempUpdate.elementAt(i));
									if (!tempHash.containsKey(tempO)) {
										tempHash.put(tempO, new Integer(tempHash.size()));
									}
								}
							}
						}
						int newIndex = tempHash.size();
						tempHash.put(ob, new Integer(newIndex));
					}
				}
			} else if (ob instanceof Double) {
				Hashtable<Object, Integer> tempHash = (Hashtable<Object, Integer>) m_cumulativeStructure
						.elementAt(i);
				if (tempHash.size() != 0) {
					if (!tempHash.containsKey(ob)) {
						int newIndex = tempHash.size();
						tempHash.put(new Double(((Double) ob).doubleValue()),
								new Integer(newIndex));
					}
				}
			} else {
				throw new Exception("Wrong object type in checkStructure!");
			}
		}
	}
	
	/**
	 * 2.1 => "2.1"
	 * 2.0 => "2"
	 * @return a String value representing the double value unless <code>val = (double)((long) val)</code>, in which case am integer string is returned 
	 */
	String doubleToString(final double val) {
		// is it special?
		if (!Double.isInfinite(val) && !Double.isNaN(val))
		{	
			// is it an integral value?
			if (val == (double) ((long) val))
				return Long.toString((long) val);
		}
		return Double.toString(val);
	}

	String doubleToString(final Double val) {
		return (val == null) ? null : doubleToString(val.doubleValue());
	}


	/**
	 * Assumes the first line of the file contains the attribute names. Assumes
	 * all attributes are real (Reading the full data set with getDataSet will
	 * establish the true structure).
	 * 
	 * @param tokenizer
	 *            a <code>StreamTokenizer</code> value
	 * @exception IOException
	 *                if an error occurs
	 * 
	 *                <pre>
	 * <jml>
	 *    private_normal_behavior
	 *      requires: tokenizer != null;
	 *      modifiable: m_structure;
	 *      ensures: m_structure != null;
	 *  also
	 *    private_exceptional_behavior
	 *      requires: tokenizer == null
	 *                || (* unsucessful parse *);
	 *      signals: (IOException);
	 * </jml>
	 * </pre>
	 */
	private void readHeader(StreamTokenizer tokenizer) throws IOException {

		FastVector attribNames = new FastVector();
		ConverterUtils.getFirstToken(tokenizer);
		if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
			ConverterUtils.errms(tokenizer, "premature end of file");
		}

		while (tokenizer.ttype != StreamTokenizer.TT_EOL) {
			attribNames.addElement(new Attribute(tokenizer.sval));
			ConverterUtils.getToken(tokenizer);
		}
		String relationName;
		if (m_sourceFile != null)
			relationName = (m_sourceFile.getName()).replaceAll(
					"\\.[cC][sS][vV]$", "");
		else
			relationName = "stream";
		m_structure = new Instances(relationName, attribNames, 0);
	}

	/**
	 * Initializes the stream tokenizer.
	 * 
	 * @param tokenizer
	 *            the tokenizer to initialize
	 */
	private void initTokenizer(StreamTokenizer tokenizer) {
		tokenizer.resetSyntax();
		tokenizer.whitespaceChars(0, (' ' - 1));
		tokenizer.wordChars(' ', '\u00FF');
		tokenizer.whitespaceChars(',', ',');
		tokenizer.whitespaceChars('\t', '\t');
		tokenizer.commentChar('%');
		tokenizer.quoteChar('"');
		tokenizer.quoteChar('\'');
		tokenizer.eolIsSignificant(true);
	}

	/**
	 * Resets the Loader ready to read a new data set or the same data set
	 * again.
	 * 
	 * @throws IOException
	 *             if something goes wrong
	 */
	@Override
	public void reset() throws IOException {
		m_structure = null;
		m_st = null;
		setRetrieval(NONE);

		if (m_File != null) {
			setFile(new File(m_File));
		}
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 6002 $");
	}

	/**
	 * Main method.
	 * 
	 * @param args
	 *            should contain the name of an input file.
	 */
	public static void main(String[] args) {
		runFileLoader(new CSVLoader(), args);
	}
}
