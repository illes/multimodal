package bme.iclef.weka;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

public class ClassifierMapper implements
	org.apache.hadoop.mapred.Mapper<Text, VectorWritable, Text, Text> {

    private static final String CLASSIFIER_FILE = "multimodal.weka.classifier.file";
    private static final String INSTANCES_FILE = "multimodal.weka.instances.file";

    Classifier classifier;

    /**
     * A possibly empty dataset, needed for attribute meta-data. Must be the
     * same as the one used to train the classifier.
     */
    Instances instances;

    @Override
    public void map(Text key, VectorWritable value,
	    OutputCollector<Text, Text> output, Reporter reporter)
	    throws IOException {
	try {
	    Instance inst = vectorToInstance(value.get());
	    sanityCheck(inst);
	    inst.setDataset(instances);
	    int classValueIndex = (int) classifier.classifyInstance(inst);
	    String classLabel = instances.classAttribute().value(classValueIndex);
	    output.collect(key, new Text(classLabel));
	} catch (Exception e) {
	    throw new RuntimeException("Error classifying instance: " + key, e);
	}
    }

    private void sanityCheck(Instance inst) {
	if (inst.numAttributes() != instances.numAttributes())
	    throw new IllegalStateException("attribute count mismatch, got: "
		    + inst.numAttributes() + ", expected: "
		    + instances.numAttributes());
    }

    /**
     * Converts a Mahout {@link Vector} to a Weka {@link Instance}. Adds an
     * empty first and last column.
     * 
     * @param v
     * @return
     */
    private static Instance vectorToInstance(Vector v) {
	final Instance inst = new Instance(v.size() + 2);
	inst.setValue(0, Instance.missingValue()); // 0: id column
	for (int i = 0; i < v.size(); i++) {
	    inst.setValue(i + 1, v.get(i)); // 1..(n-1): value columns
	}
	inst.setValue(0, inst.numAttributes() - 1); // n: label column
	return inst;
    }

    @Override
    public void configure(JobConf job) {
	try {

	    // load classifier
	    classifier = (Classifier) readSingleObject(job, CLASSIFIER_FILE,
		    null);

	    // load instances
	    instances = (Instances) readSingleObject(job, INSTANCES_FILE, null);

	    if (instances.classAttribute().index() != instances.numAttributes() - 1)
		throw new IllegalStateException(
			"Expected class attribute to be last.");

	    if (!instances.classAttribute().isNominal())
		throw new IllegalStateException(
			"Expected class attribute to be nominal.");

	} catch (Exception e) {
	    throw new RuntimeException("Error loading Weka classifier.", e);
	}
    }

    /**
     * Read a single java serialized object from a file whose path and
     * {@link FileSystem} are specified by the {@link JobConf}.
     * 
     * @param job
     * @param filenameKey
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object readSingleObject(JobConf job, String filenameKey,
	    String defaultFilename) throws IOException, ClassNotFoundException {
	ObjectInputStream ois = null;
	Object o = null;
	try {
	    FileSystem fs = FileSystem.get(job);
	    String path = job.get(filenameKey);
	    if (path == null || path.length() == 0)
		throw new IllegalStateException("Missing file name: '"
			+ filenameKey + "'");

	    ois = new ObjectInputStream(new BufferedInputStream(fs
		    .open(new Path(path))));
	    o = ois.readObject();
	    return o;
	} finally {
	    if (ois != null)
		ois.close();
	}
    }

    @Override
    public void close() throws IOException {
	classifier = null;
	instances = null;
    }

}
