package bme.iclef.hadoop;

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
import org.apache.mahout.math.NamedVector;
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
	    out.append(key, new VectorWritable(new NamedVector(value.get(),
		    key.toString())));
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
		fileOut, LongWritable.class, VectorWritable.class);

	return new ImageRecordWriter(writer);
    }
};
