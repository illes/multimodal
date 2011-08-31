package bme.iclef.weka;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SingleIndex;

public class PrecomputedClassifier extends Classifier {

	final Map<String, String> idLabel;
	final Set<String> labels;
	
	SingleIndex idIndex = new SingleIndex("first");
	SingleIndex classIndex = new SingleIndex("last");
	
	/**
	 * A value between 0 and 1.
	 * 0 is no smoothing
	 */
	double smoothFactor = 0;
	
	/**
	 * A value between 0 and 1.
	 * 0 is no smoothing
	 */
	public void setSmoothFactor(double smoothFactor) {
		if (smoothFactor < 0 || smoothFactor >= 1)
			throw new IllegalArgumentException("invalid smooth factor: " + smoothFactor);
		this.smoothFactor = smoothFactor;
	}
	
	
	public PrecomputedClassifier(Map<String, String> idLabel) {
		this.idLabel = idLabel;
		this.labels = new HashSet<String>(idLabel.values());
	}
	
	@Override
	public void buildClassifier(Instances data) throws Exception {
		idIndex.setUpper(data.numAttributes()-1);
		classIndex.setUpper(data.numAttributes()-1);
		
		if (!(data.attribute(idIndex.getIndex()).isString() || data.attribute(idIndex.getIndex()).isNominal()))
			throw new IllegalStateException("id attribute is not string or nominal (" + data.attribute(idIndex.getIndex()) +")" );
		if (!data.attribute(classIndex.getIndex()).isNominal())
			throw new IllegalStateException("class attribute is not nominal (" + data.attribute(classIndex.getIndex()) +")" );
		
		for (String label : labels)
			if (data.attribute(classIndex.getIndex()).indexOfValue(label) < 0)
				throw new IllegalStateException("class attribute missing label '"+ label +"' (" + data.attribute(classIndex.getIndex()) +")" );
	}
	
	@Override
	public double classifyInstance(Instance instance) throws Exception {
		if (smoothFactor > 0)
			return super.classifyInstance(instance);
		String id = instance.stringValue(idIndex.getIndex());
		String classLabel = idLabel.get(id);
		if (classLabel == null)
			return Instance.missingValue();
		else
			return instance.classAttribute().indexOfValue(classLabel);
	}
	
	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		if (smoothFactor <= 0)
			return super.distributionForInstance(instance);
		String id = instance.stringValue(idIndex.getIndex());
		String classLabel = idLabel.get(id);
		
		double[] out = new double[instance.numClasses()];
		if (classLabel != null)
		{
			final double weight = 1.0/(1.0 + smoothFactor*(instance.numClasses() - 1));
			final double others = (1.0 - weight)/(instance.numClasses() -1);
			Arrays.fill(out, others);
			out[instance.classAttribute().indexOfValue(classLabel)] = weight;
		}
		return out;
	}
	

}
