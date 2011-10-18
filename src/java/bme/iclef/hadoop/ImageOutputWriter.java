package bme.iclef.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.MultipleOutputFormat;
import org.apache.hadoop.util.Progressable;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

@SuppressWarnings("deprecation")
public class ImageOutputWriter extends
	MultipleOutputFormat<Text, VectorWritable> {

    static final char keyIndexSeparator = ':';

    static protected class ImageRecordWriter implements
	    RecordWriter<Text, VectorWritable> {

	private final SequenceFile.Writer out;

	public ImageRecordWriter(SequenceFile.Writer writer) {
	    out = writer;
	}

	public synchronized void write(Text key, VectorWritable value)
		throws IOException {
	    boolean nullKey = key == null;
	    boolean nullValue = value == null;

	    if (nullKey && nullValue) {
		return;
	    }

	    // out.append(key, value);
	    // Wrap the vector into a named vector with the key as name.
	    out.append(key, new VectorWritable(new NamedVector(value.get(), key
		    .toString())));
	}

	public synchronized void close(Reporter reporter) throws IOException {
	    out.close();
	}
    }

    @Override
    protected String generateFileNameForKeyValue(Text key,
	    VectorWritable value, String name) {
	/* use the key as filename */
	if (key.getClass() == Text.class) {
	    Text k = (Text) key;
	    String fName = k.toString();

	    /* handle multiple keys for one file */
	    int sepPos = -1;
	    if ((sepPos = fName.lastIndexOf(keyIndexSeparator)) != -1) {
		/* remove key index suffix */
		fName = fName.substring(0, sepPos);
	    }

	    return fName;
	}

	return super.generateFileNameForKeyValue(key, value, name);
    }

    @Override
    public RecordWriter<Text, VectorWritable> getBaseRecordWriter(
	    FileSystem fs, JobConf job, String name, Progressable arg3)
	    throws IOException {
	Path fileOut = MultipleOutputFormat.getTaskOutputPath(job, name);
	FileSystem fileSys = fileOut.getFileSystem(job);
	SequenceFile.Writer writer = SequenceFile.createWriter(fileSys, job,
		fileOut, Text.class, VectorWritable.class);

	return new ImageRecordWriter(writer);
    }

    /**
     * Test the serialization of {@link VectorWritable} with a dense vector of
     * doubles serialized as floats: [1.0f, -1.0f]. `
     * <tt>java ... | hexdump -C</tt>` yields:
     * 
     * <pre>
     * 00000000  0b 02 3f 80 00 00 bf 80  00 00                    |..?.......|
     * </pre>
     * 
     * @throws Exception
     */
    public static void test(boolean asFloat) throws Exception {
	Vector v = new DenseVector(new double[] { 1.0, -1.0 });
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	new VectorWritable();
	VectorWritable.writeVector(new DataOutputStream(out), v, asFloat);
	byte[] buffer = out.toByteArray();

	System.out.write(buffer);
	// final char[] HEX_CHAR = new char[] { '0', '1', '2', '3', '4', '5',
	// '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	//
	//
	//
	// StringBuffer sb = new StringBuffer();
	//
	// for ( int i = 0; i < buffer.length; i++ )
	// {
	// sb.append( "0x" ).append( ( char ) ( HEX_CHAR[( buffer[i] & 0x00F0 )
	// >> 4] ) ).append(
	// ( char ) ( HEX_CHAR[buffer[i] & 0x000F] ) ).append( " " );
	// }

    }

    public static void main(String[] args) throws Exception {
	test(true);
    }
};
