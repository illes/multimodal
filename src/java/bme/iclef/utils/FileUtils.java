package bme.iclef.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileUtils {
	

	/**
	 * This file filter accepts only normal files, e.g. no directories. See java.io.File.isFile().
	 */
	static FilenameFilter onlyFilenamesFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			File thefile = new File(dir.getAbsolutePath() + "/" + name);
			return thefile.isFile();
		}
	};

	/**
	 * This file filter accepts only normal files that additionally match a file filter expression. Get and set
	 * the current file filter expression with <tt>(g|s)etFilefilterExpression</tt>. Default: &quot;<tt>.*</tt>&quot;
	 */
	static FilenameFilter onlyFilenamesFilterWithExpression = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			File thefile = new File(dir.getAbsolutePath() + "/" + name);
			return (thefile.isFile() && name.matches(filefilterExpression));
		}
	};

	/**
	 * This file filter accepts only directories. See  java.io.File.isDirectory().
	 */
	static FilenameFilter onlyDirectorynamesFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			File thefile = new File(dir.getAbsolutePath() + "/" + name);
			return thefile.isDirectory();
		}
	};

	private static String filefilterExpression = ".*";

	
	private static abstract class PatternFilenameFilter implements FilenameFilter
	{
			private final Matcher m;

			public PatternFilenameFilter(Pattern pattern)
			{
				this.m = pattern.matcher("");
			}
			
			public PatternFilenameFilter(String regex)
			{
				this(Pattern.compile(regex));
			}

			/**
			 * 
			 * @param filename
			 * @return <code>true</code> iff the pattern matches the <b>whole</b> filename
			 */
			protected boolean matches(String filename)
			{
				return m.reset(filename).matches();
			}
	}

	/**
 	 * Accepts all files whose whole basename is <b>not</b> matched by the given regular expression. 
	 * @author illes
	 *
	 */
	public static  class ExcludePatternFilenameFilter extends PatternFilenameFilter
	{
		public ExcludePatternFilenameFilter(String regex) {
			super(regex);
		}

		@Override
		public boolean accept(File dir, String name) {
			return !super.matches(name);
		}
	}

	/**
	 * Accepts all files whose <b>whole</b> basename is matched by the given regular expression. 
	 * @author illes
	 *
	 */
	public static  class IncludePatternFilenameFilter extends PatternFilenameFilter
	{

		public IncludePatternFilenameFilter(String regex) {
			super(regex);
		}

		@Override
		public boolean accept(File dir, String name) {
			System.err.println("Checking " + name);
			return super.matches(name);
		}
		
	}

	/**
	 * @throws FileNotFoundException
	 * */
	public static BufferedWriter getBufferedFileWriter(File file, Charset cs) throws  FileNotFoundException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), cs));
	}

	/**
	 * Write the string into a file.
	 * @param content
	 * @param filename
	 * @throws IOException 
	 */
	public static void writeToFile (String content, File file, java.nio.charset.Charset cs) throws IOException {
		Writer w = getBufferedFileWriter(file, cs);
		w.write(content);
		w.close();
	}


}
