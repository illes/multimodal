package bme.iclef.weka;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SingleIndex;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

public class HierarchicalClassifier extends Classifier {

	private static final long serialVersionUID = -7246133345390066741L;

	public static class HierachyNode implements Serializable {
		private static final long serialVersionUID = 1L;
		final String label;
		Set<HierachyNode> children;

		public HierachyNode(String label) {
			this.label = label;
			this.children = new HashSet<HierachyNode>();
		}

		public String getLabel() {
			return label;
		}

		public void addChild(HierachyNode hn) {
			children.add(hn);
		}

		public Set<String> getLabels() {
			if (children == null || children.isEmpty())
				return Collections.singleton(label);

			Set<String> labels = new HashSet<String>();
			for (HierachyNode child : children)
				labels.addAll(child.getLabels());
			return labels;
		}

		public Set<HierachyNode> getChildren() {
			return children;
		}

		@Override
		public String toString() {
			return "("
					+ label
					+ (children.isEmpty() ? "" : (":" + Arrays
							.toString(children.toArray()))) + ")";
		}
		
		public boolean isPreTerminal()
		{
			if (children.isEmpty())
				return false;
			for (HierachyNode child : children)
				if (!child.isTerminal())
					return false;
			return true;
		}

		public HierachyNode getChild(String label) {
			if (label == null)
				throw new NullPointerException();
			for (HierachyNode child : children)
				if (child.label.equals(label))
					return child;
			return null;
		}

		public boolean isTerminal() {
			return children.isEmpty();
		}
	}
	

	final Classifier baseClassifier;
	final HierachyNode root;
	final Map<HierachyNode, Classifier> subClassifiers;
	final Map<HierachyNode, MapNominalFilter> subMappingFilters;
	
	/**
	 * Whether to follow the best path only, or  aggregate over all paths.
	 */
	private boolean greedy = true;

	public HierarchicalClassifier(Classifier baseClassifier, HierachyNode root) {
		this.baseClassifier = baseClassifier;
		this.subClassifiers = new HashMap<HierachyNode, Classifier>();
		this.subMappingFilters = new HashMap<HierachyNode, MapNominalFilter>();
		this.root = root;
	}

	protected MapNominalFilter buildMappingFilter(HierachyNode hn,
			Instances data) throws Exception {
		Map<String, String> labelMapping = new HashMap<String, String>();
		for (HierachyNode child : hn.children)
			for (String label : child.getLabels()) {
				labelMapping.put(label, child.getLabel());
//				System.err.println("DEBUG: In node '" + hn.getLabel() + "', mapping '"
//						+ label + "' to '" + child.getLabel() + "'");
			}

		MapNominalFilter mnf = new MapNominalFilter();
		mnf.setAttribute(data.classIndex());
		mnf.setMapping(labelMapping);
		mnf.setInputFormat(data);

		// ClassAssigner ca = new ClassAssigner();
		// ca.setClassIndex(String.valueOf(data.classIndex()+1));
		//		
		// MultiFilter mf = new MultiFilter();
		// mf.setFilters(new Filter[] {mnf, ca});

		return mnf;
	}

	public static class MapNominalFilter extends Filter implements
			UnsupervisedFilter, StreamableFilter {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7403042043980354064L;
		private SingleIndex index = new SingleIndex("last");
		private Map<String, String> mapping = Collections.emptyMap();
		private FastVector attributeValues;

		public void setAttribute(int index) {
			this.index = new SingleIndex(String.valueOf(index + 1));
		}

		public void setAttribute(SingleIndex index) {
			this.index = index;
		}

		public void setMapping(Map<String, String> mapping) {
			this.mapping = mapping;
		}

		private FastVector nominalLabelValues() {
			FastVector fv = new FastVector();
			for (String label : new TreeSet<String>(mapping.values())) {
				fv.addElement(label);
			}
			return fv;
		}

		public String getMappedValue(int valueIndex) {
			return (String) attributeValues.elementAt(valueIndex);
		}

		@Override
		public boolean setInputFormat(Instances instanceInfo) throws Exception {
			index.setUpper(instanceInfo.numAttributes() - 1);
			if (instanceInfo.attribute(index.getIndex()).type() != Attribute.NOMINAL)
				throw new IllegalStateException("Attribute " + index
						+ " is not NOMINAL ("
						+ instanceInfo.attribute(index.getIndex()) + ")");
			super.setInputFormat(instanceInfo);

			this.attributeValues = nominalLabelValues();
			// Remove r = new Remove();
			// r.setAttributeIndicesArray(new int[] {index.getIndex()});
			// Add a = new Add();
			// a.setAttributeIndex(index.getSingleIndex());
			// a.setAttributeName(instanceInfo.attribute(index.getIndex()).name());
			// a.setNominalLabels(nominalLabels());
			//			
			// super.setFilters(new Filter[] {r, a});
			// return super.setInputFormat(instanceInfo);

			final boolean isClassAttribute = index.getIndex() == instanceInfo
					.classIndex();

			Instances outputFormat = new Instances(instanceInfo, 0);
			if (isClassAttribute)
				// outputFormat.setClass(new Attribute(null)); // hack to unset
				// class index
				outputFormat.setClassIndex(-1);
			outputFormat.deleteAttributeAt(index.getIndex());
			outputFormat.insertAttributeAt(new Attribute(instanceInfo
					.attribute(index.getIndex()).name(), attributeValues),
					index.getIndex());
			if (isClassAttribute)
				outputFormat.setClassIndex(index.getIndex());

			final boolean ret = super.setInputFormat(instanceInfo);
			setOutputFormat(outputFormat);
			return ret;
		}

		@Override
		public boolean input(Instance inst) throws Exception {
			if (getInputFormat() == null) {
				throw new IllegalStateException(
						"No input instance format defined");
			}
			if (m_NewBatch) {
				resetQueue();
				m_NewBatch = false;
			}
			String srcLabel = inst.stringValue(index.getIndex());
			String destLabel = mapping.get(srcLabel);

			inst = (Instance) inst.copy();
			inst.setMissing(index.getIndex());
			inst.setDataset(null);
			inst.setDataset(getOutputFormat());
			if (destLabel != null)
				inst.setValue(index.getIndex(), destLabel);
			//System.err.println("DEBUG: mapped from '" + srcLabel + "' to '" + destLabel + "'");
			push(inst);
			return true;
		}
	}

	@Override
	public void buildClassifier(Instances data) throws Exception {
		subClassifiers.clear();
		System.err.println("INFO: Building classifiers ...");
		buildClassifierForNodeRecursive(data, root);
		System.err.println("INFO: Done building classifiers.");
	}

	/**
	 * Recursively build classifiers for a {@link HierachyNode}.
	 * 
	 * @param data
	 * @param hn
	 * @throws Exception
	 */
	protected void buildClassifierForNode(Instances data, HierachyNode hn)
			throws Exception {
		Classifier c = Classifier.makeCopy(baseClassifier);

		System.err.println("INFO: Building classifier for node '"
				+ hn.getLabel() + "' ...");
		if (data.classAttribute().type() != Attribute.NOMINAL)
			throw new IllegalStateException("Attribute " + data.classIndex()
					+ " is not NOMINAL (" + data.classAttribute() + ")");

		MapNominalFilter f = buildMappingFilter(hn, data);

		c.buildClassifier(Filter.useFilter(data, f));

		subClassifiers.put(hn, c);
		subMappingFilters.put(hn, f);
	}

	protected void buildClassifierForNodeRecursive(Instances data,
			HierachyNode hn) throws Exception {
		buildClassifierForNode(data, hn);

		if (hn.getChildren() != null)
			for (HierachyNode child : hn.getChildren())
				buildClassifierForNode(data, child);
	}

	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		return classifyAscend(instance);
	}

	private double[] classifyAscend(Instance instance) throws Exception {
		return classifyAscend(instance, root, 1.0);
	}

	private double[] classifyAscend(Instance instance, HierachyNode hn, double probability)
			throws Exception {
		// double[] dist = new double[instance.numClasses()];
		Classifier c = subClassifiers.get(hn);
		MapNominalFilter f = subMappingFilters.get(hn);

		final Instance mappedInst;
		{
			if (!f.input(instance))
				throw new IllegalStateException(
						"The filter was not immediately ready.");
			mappedInst = f.output();
		}
		double[] mappedDistribution = c.distributionForInstance(mappedInst);

		final double[] distribution;
		if (hn.isPreTerminal()) {
			
			distribution = new double[instance.numClasses()];
			for (int i = 0; i < mappedDistribution.length; i++) {
				final String label = mappedInst.classAttribute().value(i);
				final int labelIndex = instance.classAttribute().indexOfValue(label);
				if (labelIndex < 0)
					throw new IllegalStateException("could not find terminal label '" + label + "' in attribute '" + instance.classAttribute()+"'");
				distribution[labelIndex] = mappedDistribution[i];
			}
			
		} else if (hn.isTerminal()) {
			
			distribution = new double[instance.numClasses()];
			distribution[instance.classAttribute().indexOfValue(hn.label)] = 1;
			
		} else {
			
			int maxIndex = 0;

			if (greedy)
			{
				for (int i = 0; i < mappedDistribution.length; i++) {
					if (mappedDistribution[maxIndex] < mappedDistribution[i])
						maxIndex = i;
					// System.err.println("INFO: " + i + ":" +
					// (float)mappedDistribution[i] + " " +
					// mappedInst.classAttribute().value(i));
				}

				final String maxLabel = mappedInst.classAttribute().value(maxIndex);
				
				distribution = classifyAscend(instance, hn.getChild(maxLabel), 1.0);
			}
			else
			{
				distribution = new double[instance.numClasses()];
				for (int i = 0; i < mappedDistribution.length; i++) {
					final String label = mappedInst.classAttribute().value(i);
					add(distribution, classifyAscend(instance, hn.getChild(label), mappedDistribution[i]));
				}
			}
		}
		scale(distribution, probability);
		return distribution;
	}
	
	private void add(double[] inout, double[] in) {
		for (int i = 0; i < inout.length; i++) {
			inout[i] += in[i];
		}
	}
	
	private void scale(double[] inout, double coefficient) {
		for (int i = 0; i < inout.length; i++) {
			inout[i] *= coefficient;
		}
	}

	@Override
	public String[] getOptions() {
		List<String> options = new ArrayList<String>();
		options.add(root.toString());
		options.add(baseClassifier.getClass().getCanonicalName());
		String[] baseClassifierOptions = baseClassifier.getOptions();
		for (int i = 0; i < baseClassifierOptions.length; i++) {
			options.add(baseClassifierOptions[i]);
		}
		return options.toArray(new String[options.size()]);
	}
}
