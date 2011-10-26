package bme.iclef.hadoop;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.SequenceFile.Sorter;

import com.google.common.io.Files;

/**
 * Joins key/value pairs in sequence-format files.
 * 
 * <p>
 * Performs sort and an inner-loop join on any number of input files. Input
 * files must have the same key type, support for differing value types not
 * implemented yet.
 * 
 * @see SequenceFile.Sorter
 * @author Illes Solt
 * 
 */
@SuppressWarnings("unchecked")
public class SequenceFileJoiner {

    private final Configuration conf;
    private final FileSystem fs;
    private final Class<? extends WritableComparable> keyClass;
    private final Class<? extends Writable> valClass;

    public SequenceFileJoiner(FileSystem fs, Configuration conf,
	    Class<? extends WritableComparable> keyClass,
	    Class<? extends Writable> valClass) {
	this.fs = fs;
	this.conf = conf;
	this.keyClass = keyClass;
	this.valClass = valClass;
    }

    public JoinedKeyValuesIterator joinIterate(Path[] paths)
	    throws IOException, InstantiationException, IllegalAccessException {

	return new JoinedKeyValuesIterator(paths);
    }

    private class JoinedKeyValuesIterator {

	/**
	 * Current key.
	 */
	WritableComparable key;
	/**
	 * Current values corresponding to key, <code>null</code> if current key
	 * is not present in ith seq file.
	 */
	Writable[] value;

	/**
	 * Current keys of sorters.
	 */
	final WritableComparable keys[];
	/**
	 * Current values of sorters.
	 */
	final Writable values[];

	final Sorter sorter;
	final SequenceFile.Sorter.RawKeyValueIterator[] iterators;

	/**
	 * Whether the objects in <code>keys</code> and <code>values</code> are
	 * valid.
	 */
	final BitSet hasKeyValue;

	final DataOutputBuffer outBuf = new DataOutputBuffer();
	final DataInputBuffer inBuf = new DataInputBuffer();

	final boolean[] isMin;

	public JoinedKeyValuesIterator(Path[] paths) throws IOException,
		InstantiationException, IllegalAccessException {
	    sorter = new Sorter(fs, keyClass, valClass, conf);
	    iterators = new SequenceFile.Sorter.RawKeyValueIterator[paths.length];
	    hasKeyValue = new BitSet(paths.length);

	    keys = new WritableComparable[paths.length];
	    values = new Writable[paths.length];
	    value = new Writable[paths.length];
	    isMin = new boolean[paths.length];
	    for (int i = 0; i < paths.length; i++) {
		iterators[i] = sorter.sortAndIterate(new Path[] { paths[i] },
			new Path(Files.createTempDir().getAbsolutePath()),
			false);

		keys[i] = keyClass.newInstance();
		values[i] = valClass.newInstance();

		next(i);
	    }
	}

	/**
	 * Gets the current key.
	 * 
	 * @return
	 */
	public WritableComparable getKey() {
	    return key;
	}

	/**
	 * Gets the values. <code>null</code> values indicate that getKey is
	 * missing from the <i>i</i>th sequence file.
	 */
	public Writable[] getValue() {
	    return value;
	}

	/**
	 * Advances <i>i</i>th iterator and deserialize key/value.
	 * 
	 * @param i
	 * @throws IOException
	 */
	private void next(int i) throws IOException {
	    hasKeyValue.set(i, iterators[i].next());
	    if (hasKeyValue.get(i)) {
		// read key
		inBuf.reset(iterators[i].getKey().getData(), iterators[i]
			.getKey().getData().length);
		keys[i].readFields(inBuf);

		// read value
		outBuf.reset();
		iterators[i].getValue().writeUncompressedBytes(outBuf);
		inBuf.reset(outBuf.getData(), outBuf.getLength());
		values[i].readFields(inBuf);
	    }
	}

	/**
	 * Sets up the current key and value (for getKey and getValue)
	 * 
	 * @return <code>true</code> if there exists a key/value,
	 *         <code>false</code> otherwise
	 * @throws IOException
	 */
	public boolean next() {
	    for (int i = 0; i < isMin.length; i++) {
		if (isMin[i]) {
		    try {
			next(i);
		    } catch (IOException e) {
			throw new RuntimeException(
				"error advancing sorted seqfile", e);
		    }
		    isMin[i] = false;
		}
	    }

	    // find all minimum keys
	    int minIndex = -1;
	    for (int i = 0; i < keys.length; i++) {
		if (!hasKeyValue.get(i))
		    continue;

		if (minIndex < 0) {
		    isMin[minIndex = i] = true;
		    continue;
		}

		final int cmp = keys[minIndex].compareTo(keys[i]);
		if (cmp > 0) {
		    Arrays.fill(isMin, minIndex, i, false); // same effect as
		    // Arrays.fill(isMin,
		    // false)
		    isMin[minIndex = i] = true;
		} else if (cmp == 0) {
		    isMin[i] = true;
		}
	    }

	    if (minIndex < 0)
		return false;

	    // fill entry
	    key = keys[minIndex];
	    Arrays.fill(value, null);
	    for (int i = 0; i < isMin.length; i++) {
		if (isMin[i]) {
		    value[i] = values[i];
		}
	    }

	    return true;
	}
    }

    public static void main(String[] args) throws IOException,
	    InstantiationException, IllegalAccessException {
	Configuration conf = new Configuration();
	FileSystem fs = FileSystem.get(conf);
	fs.setConf(conf);

	Path paths[] = new Path[] { new Path("/tmp/1.seq"),
		new Path("/tmp/2.seq"), new Path("/tmp/3.seq") };
	{
	    SequenceFile.Writer w = SequenceFile.createWriter(fs, conf,
		    paths[0], LongWritable.class, Text.class);
	    w.append(new LongWritable(3), new Text("three"));
	    w.append(new LongWritable(5), new Text("five"));
	    w.append(new LongWritable(1), new Text("one"));
	    w.close();
	}
	{
	    SequenceFile.Writer w = SequenceFile.createWriter(fs, conf,
		    paths[1], LongWritable.class, Text.class);
	    w.append(new LongWritable(1), new Text("Eins"));
	    w.append(new LongWritable(7), new Text("Sieben"));
	    w.append(new LongWritable(3), new Text("Drei"));
	    w.append(new LongWritable(2), new Text("Zwei"));
	    w.close();
	}
	{
	    SequenceFile.Writer w = SequenceFile.createWriter(fs, conf,
		    paths[2], LongWritable.class, Text.class);
	    w.append(new LongWritable(5), new Text("cinque"));
	    w.append(new LongWritable(7), new Text("sept"));
	    w.close();
	}
	
	// print raw
	System.out.println("Individual files:");
	for (int i = 0; i < paths.length; i++) {
	    System.out.println(paths[i] +":");
	    SequenceFile.Reader reader = new SequenceFile.Reader(fs, paths[i], conf);
	    LongWritable key = new LongWritable(); 
	    Text value = new Text(); 
	    while (reader.next(key, value))
		System.out.println(key + " => " + value);
	}

	// print joined
	System.out.println("Joined:");
	SequenceFileJoiner joiner = new SequenceFileJoiner(fs, conf,
		LongWritable.class, Text.class);
	for (JoinedKeyValuesIterator iterator = joiner.joinIterate(paths); iterator
		.next();) {
	    System.out.println(iterator.getKey().toString() + " => "
		    + Arrays.toString(iterator.getValue()));
	}
    }
}
