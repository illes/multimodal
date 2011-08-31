package bme.iclef.weka;

import gnu.trove.TIntArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.Vote;
import weka.classifiers.functions.GaussianProcesses;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.SingleIndex;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;
import bme.iclef.predict.Prediction.Label;
import bme.iclef.utils.Maps;

public class Driver {
	public static void main(String[] args) {

		final boolean precomputed = false;

		Instances trainData = null, testData = null;
		final boolean crossValidation = args.length == 1;

		try {
			String dataFile = args[0];
			System.err.println("INFO: Loading dataset from '" + dataFile
					+ "' ...");

			CSVLoader csvLoader = new CSVLoader();
			//csvLoader.setStringAttributes("first"); // id
			//csvLoader.setNominalAttributes("last"); // label
			csvLoader.setNominalAttributes("first,last"); // id, label
			csvLoader.setSource(new FileInputStream(dataFile));

			Instances data = csvLoader.getDataSet();

			if (!crossValidation) {
				System.err.println("INFO: Loading splits from '" + args[1]
						+ "' (train), '" + args[2] + "' (test) ...");
				Set<String> trainLabels = Collections
						.unmodifiableSet(new HashSet<String>(
								readAsList(args[1])));
				Set<String> testLabels = Collections
						.unmodifiableSet(new HashSet<String>(
								readAsList(args[2])));
				{
					Set<String> intersection = new HashSet<String>(trainLabels);
					intersection.retainAll(testLabels);
					if (!intersection.isEmpty())
						throw new IllegalStateException(
								"Train and test sets intersect: "
										+ Arrays.toString(intersection
												.toArray()));
				}
				RemoveWithStringValues rwsv = new RemoveWithStringValues();
				rwsv.setAttributeIndex("first");
				trainData = rwsv.setValues(trainLabels)
						.split(data, true, false);
				testData = rwsv.setValues(testLabels).split(data, true, false);

				if (trainData.classIndex() == -1)
					trainData.setClassIndex(trainData.numAttributes() - 1);
				if (testData.classIndex() == -1)
					testData.setClassIndex(testData.numAttributes() - 1);

				data = null;
			} else if (!precomputed) {
				if (data.classIndex() == -1)
					data.setClassIndex(data.numAttributes() - 1);
				data.deleteWithMissingClass();
			}

			// feature selection
			// if (crossValidation)
			// {
			// InfoGainFeatureSelection igfs = new InfoGainFeatureSelection(10,
			// true);
			// igfs.build(data);
			//				
			// System.err.println("INFO: Selected " +
			// igfs.selectedAttributes().size() +" of " +
			// (data.numAttributes()-2) + " attributes.");
			//				
			// Remove r = new Remove();
			// r.setAttributeIndices("first," + igfs.selectedAttributeRange()
			// +",last");
			// r.setInvertSelection(true);
			//				
			// r.setInputFormat(data);
			// data = Filter.useFilter(data, r);
			// if (data.classIndex() == -1)
			// data.setClassIndex(data.numAttributes() - 1);
			// }

			// standardize
			// {
			// Standardize st = new Standardize();
			//				
			// st.setInputFormat(data);
			// data = Filter.useFilter(data, st);
			// if (data.classIndex() == -1)
			// data.setClassIndex(data.numAttributes() - 1);
			// }

			// weighting to balance classes
			// {
			// System.err.println("INFO: Weighting instances...");
			// // collect statistics
			// int[] classSupport = new int[data.numClasses()];
			// int hasClass = 0;
			// for (int i = 0; i < data.numInstances(); i++) {
			// if (data.instance(i).classIsMissing())
			// continue;
			// classSupport[(int) data.instance(i).classValue()]++;
			// hasClass++;
			// }
			//
			// // calculate weights
			// double[] classWeight = new double[data.numClasses()];
			// final int smoothFactor = 2;
			// double expectedFrequency = 1.0d / data.numClasses();
			// for (int i = 0; i < data.numClasses(); i++) {
			// final double frequency = classSupport[i]
			// / (double) hasClass;
			// final double ratio = expectedFrequency / frequency;
			// classWeight[i] = (smoothFactor + ratio)/(smoothFactor + 1);
			// System.err.println("INFO: Class '" +
			// data.classAttribute().value(i) + "' instance weight set to " +
			// (float)classWeight[i] );
			// }
			//
			// for (int i = 0; i < data.numInstances(); i++) {
			// if (data.instance(i).classIsMissing())
			// continue;
			// Instance inst = data.instance(i);
			// inst.setWeight(classWeight[(int) inst.classValue()]);
			// }
			//
			// }

			if (crossValidation) {
				for (int i = 0; i < data.numAttributes(); i++) {
					if (i == 0 || i == data.classIndex())
						continue;
					Attribute a = data.attribute(i);
					if (!a.isNumeric())
						throw new IllegalStateException(
								"attribute is not numeric: " + a);
				}
			}

			final Classifier c;
			{
				if (!precomputed) {
					// classifier
					SMO smo = new SMO();
					// c = smo;

					FilteredClassifier smoRegex = prefixFilteredClassifier(
							new SMO(), "regex_");

					HackedClassifier smoRegexHacked = new HackedClassifier();
					smoRegexHacked.setSureClasses(Collections
							.singleton(Label.DM_Dermatology.code));
					smoRegexHacked.setClassifier(smoRegex);

					PrecomputedClassifier pc = new PrecomputedClassifier(
							Maps
									.readStringStringMapFromFile(
											new File("resources/classificationResults_sampleRun.train+test.csv"),
											Charset.forName("US-ASCII"), ",",
											new HashMap<String, String>(), true, 1));					
					pc.setSmoothFactor(0.7);
					weka.classifiers.meta.Vote v = new Vote();
					v.setOptions(new String[] { "-R", "AVG" });
					v.setClassifiers(new Classifier[] { wrapRemoveFirst(smo), wrapRemoveFirst(smoRegexHacked), pc });
					

//					c = v;
					c = wrapRemoveFirst(smo);
					
					// GaussianProcesses gp = new GaussianProcesses ();
					// c = gp;

					// LADTree t = new LADTree();
					// c = t;

					// LibSVM svm = new LibSVM();
					// c = svm;

					// JRip jrip = new JRip();
					// c = jrip;

					// LogitBoost lb = new LogitBoost();
					// lb.setClassifier(new SMOreg());
					// lb.setDebug(true);
					// c = lb;
					// BayesNet bn = new BayesNet();
					// // c = bn;
					//
					// J48 j48 = new J48();
					// // j48.setMinNumObj(10);
					// // c = j48;
					//
					// ClassificationViaRegression cvr = new
					// ClassificationViaRegression();
					// cvr.setClassifier(new SMOreg());
					// c = cvr;

					// Vote v = new Vote();
					// v.setClassifiers(new Classifier[] {smo, bn, j48});
					// c = v;

					// // HIERARCHY -- groups
					// HierachyNode root = new HierachyNode("ROOT");
					// Map<Group, HierachyNode> groupNodes = new HashMap<Group,
					// HierachyNode>();
					// for (Group g : Group.values()) {
					// HierachyNode groupNode = new HierachyNode(g.name());
					// root.addChild(groupNode);
					// groupNodes.put(g, groupNode);
					// }
					// for (Label l : Label.values()) {
					// if (l.code != null) {
					// HierachyNode labelNode = new HierachyNode(l.code);
					// groupNodes.get(l.group).addChild(labelNode);
					// } else
					// System.err
					// .println("WARNING: Skipping label without code  "
					// + l);
					// }
					// System.err.println(root.toString());
					//
					// HierarchicalClassifier hc = new
					// HierarchicalClassifier(smo,
					// root);
					// c = hc;

					// // HIERARCHY -- custom
					// HierachyNode root = new HierachyNode("ROOT");
					// HierachyNode radiology = new HierachyNode("RADIOLOGY");
					// HierachyNode graphic = new HierachyNode("GRAPHIC");
					// HierachyNode photo = new HierachyNode("PHOTO");
					// root.addChild(radiology);
					// root.addChild(graphic);
					// root.addChild(photo);
					// for (Label l : Label.values()) {
					// if (l.code != null) {
					// HierachyNode labelNode = new HierachyNode(l.code);
					//							
					// switch (l) {
					// case _3D_ThreeDee:
					// graphic.addChild(labelNode);
					// break;
					// case AN_Angiography:
					// radiology.addChild(labelNode);
					// break;
					// case CM_CompoundFigure:
					// graphic.addChild(labelNode);
					// break;
					// case CT_ComputedTomography:
					// radiology.addChild(labelNode);
					// break;
					// case DM_Dermatology:
					// graphic.addChild(labelNode);
					// break;
					// case DR_Drawing:
					// graphic.addChild(labelNode);
					// break;
					// case EM_ElectronMicroscopy:
					// photo.addChild(labelNode);
					// break;
					// case EN_Endoscope:
					// photo.addChild(labelNode);
					// break;
					// case FL_Fluorescense:
					// radiology.addChild(labelNode);
					// break;
					// case GL_Gel:
					// graphic.addChild(labelNode);
					// break;
					// case GR_GrossPathology:
					// photo.addChild(labelNode);
					// break;
					// case GX_Graphs:
					// graphic.addChild(labelNode);
					// break;
					// case HX_Histopathology:
					// radiology.addChild(labelNode);
					// break;
					// case MR_MagneticResonance:
					// radiology.addChild(labelNode);
					// break;
					// case PX_Photo:
					// photo.addChild(labelNode);
					// break;
					// case RN_Retinograph:
					// radiology.addChild(labelNode);
					// break;
					// case US_Ultrasound:
					// radiology.addChild(labelNode);
					// break;
					// case XR_XRay:
					// radiology.addChild(labelNode);
					// break;
					// default:
					// throw new IllegalStateException(l.toString());
					// }
					//							
					// } else
					// System.err
					// .println("WARNING: Skipping label without code  "+ l);
					// }
					// System.err.println(root.toString());
					//
					// HierarchicalClassifier hc = new
					// HierarchicalClassifier(smo,
					// root);
					// c = hc;

					// Cluster membership

					// EM em = new EM();
					// em.setNumClusters(Label.values().length); // TODO
					// heuristics
					//					
					// ClusterMembership cm = new AddClusterMembership();
					// cm.setDensityBasedClusterer(em);
					// cm.setIgnoredAttributeIndices(String.valueOf(data.classIndex()+1));
					// // ignore class label
					//					
					// FilteredClassifier fc = new FilteredClassifier();
					// fc.setClassifier(hc);
					// fc.setFilter(cm);
					//					
					// c = fc;

					// ASEvaluation ae = new InfoGainAttributeEval();
					//					
					// Ranker ranker = new Ranker();
					// ranker.setNumToSelect(data.numAttributes()/2);
					// AttributeSelectedClassifier asc = new
					// AttributeSelectedClassifier();
					// asc.setClassifier(smo);
					// asc.setEvaluator(ae);
					// asc.setSearch(ranker);
					//					
					// c = asc;
				} else {
					c = new PrecomputedClassifier(
							Maps
									.readStringStringMapFromFile(
											new File(
													"resources/classificationResults_sampleRun.txt"),
											Charset.forName("US-ASCII"), " ",
											new HashMap<String, String>(), true));
				}
			}

			c.setDebug(true);

			if (precomputed)
				evaluateClassifier(c, data, 10);
			else if (crossValidation)
				evaluateClassifier(c, data, 10);
			else
				// split
				evaluateClassifier(c, trainData, testData);

		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	static FilteredClassifier wrapRemoveFirst(Classifier c)
	{
		Remove remove = new Remove(); // new instance of filter
		remove.setAttributeIndices("first");
		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(remove);
		fc.setClassifier(c);
		fc.setDebug(true);
		return fc;
	}

	private static FilteredClassifier prefixFilteredClassifier(Classifier c,
			String prefix) {
		RemoveWithPrefix rwp = new RemoveWithPrefix();
		rwp.setMatchClass(true);
		rwp.setPrefix(prefix);
		rwp.setInvertSelection(true);

		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(c);
		fc.setFilter(rwp);

		return fc;
	}

	private static List<String> readAsList(String fileName) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(fileName));
		String line;
		List<String> out = new ArrayList<String>();
		while ((line = r.readLine()) != null)
			out.add(line);

		return out;
	}

	static void evaluateClassifier(Classifier c, Instances data, int folds)
			throws Exception {
		System.err.println("INFO: Starting crossvalidation to predict '"
				+ data.classAttribute().name() + "' using '"
				+ c.getClass().getCanonicalName() + ":"
				+ Arrays.toString(c.getOptions()) + "' ...");

		StringBuffer sb = new StringBuffer();
		Evaluation eval = new Evaluation(data);
		eval.crossValidateModel(c, data, folds, new Random(1), sb, new Range(
				"first"), Boolean.FALSE);

		// write predictions to file
		{
			Writer out = new FileWriter("cv.log");
			out.write(sb.toString());
			out.close();
		}

		System.out.println(eval.toClassDetailsString());
		System.out.println(eval.toSummaryString("\nResults\n======\n", false));
	}

	private static void evaluateClassifier(Classifier c, Instances trainData,
			Instances testData) throws Exception {
		System.err.println("INFO: Starting split validation to predict '"
				+ trainData.classAttribute().name() + "' using '"
				+ c.getClass().getCanonicalName() + ":"
				+ Arrays.toString(c.getOptions()) + "' (#train="
				+ trainData.numInstances() + ",#test="
				+ testData.numInstances() + ") ...");

		if (trainData.classIndex() < 0)
			throw new IllegalStateException("class attribute not set");

		c.buildClassifier(trainData);
		Evaluation eval = new Evaluation(testData);
		eval.useNoPriors();
		double[] predictions = eval.evaluateModel(c, testData);
		
		System.out.println(eval.toClassDetailsString());
		System.out.println(eval.toSummaryString("\nResults\n======\n", false));

		// write predictions to file
		{
			System.err.println("INFO: Writing predictions to file ...");
			Writer out = new FileWriter("prediction.trec");
			writePredictionsTrecEval(predictions, testData, 0, trainData
					.classIndex(), out);
			out.close();
		}
		
		// write predicted distributions to CSV
		{
			System.err.println("INFO: Writing predicted distributions to CSV ...");
			Writer out = new FileWriter("predicted_distribution.csv");
			writePredictedDistributions(c, testData, 0, out);
			out.close();
			
		}
	}

	private static void writePredictedDistributions(Classifier c,
			Instances data, int idIndex, Writer out) throws Exception 
	{
		// header
		out.write("id");
		for (int i = 0; i < data.numClasses(); i++)
		{
			out.write(",\"");
			out.write(data.classAttribute().value(i).replaceAll("[\"\\\\]", "_"));
			out.write("\"");
		}
		out.write("\n");
		
		// data
		for (int i = 0; i < data.numInstances(); i++) {
			final String id = data.instance(i).stringValue(idIndex);
			double[] distribution = c.distributionForInstance(data.instance(i));
			
			//final String label = data.attribute(classIndex).value();
			out.write(id);
			for (double probability : distribution) {
				out.write(",");
				out.write(String.valueOf(probability > 1e-5 ? (float)probability : 0f));
			}
			out.write("\n");
		}
	}

	private static void writePredictionsTrecEval(double[] predictions,
			Instances data, int idIndex, int classIndex, Writer out)
			throws IOException {
		if (predictions.length != data.numInstances())
			throw new IllegalStateException(predictions.length + "!="
					+ data.numInstances());
		for (int i = 0; i < predictions.length; i++) {
			final String id = data.instance(i).stringValue(idIndex);
			final String label = data.attribute(classIndex).value(
					(int) predictions[i]);
			out.write(id);
			out.write(" ");
			out.write(label);
			out.write(" 1.0\n");
		}
	}

	private static void writePredictionsDebug(double[] predictions,
			Instances data, int idIndex, int classIndex, Writer out)
			throws IOException {
		if (predictions.length != data.numInstances())
			throw new IllegalStateException(predictions.length + "!="
					+ data.numInstances());
		for (int i = 0; i < predictions.length; i++) {
			final String id = data.instance(i).stringValue(idIndex);
			final String trueLabel = data.instance(i).stringValue(classIndex);
			final String predictedLabel = data.attribute(classIndex).value(
					(int) predictions[i]);
			out.write(id);
			out.write("\t");
			out.write(trueLabel);
			out.write("\t");
			out.write(predictedLabel);
			out.write("\t1.0\n");
		}
	}

	/**
	 * Class to split weka Instances based on value of an attribute (e.g.,
	 * "fold").
	 * 
	 */
	static class RemoveWithStringValues extends RemoveWithValues {

		private static final long serialVersionUID = 1L;
		private Set<String> values;

		@Override
		public boolean setInputFormat(Instances instanceInfo) throws Exception {
			super
					.setNominalIndicesArr(toValueIndexArray(getAttribute(instanceInfo)));
			return super.setInputFormat(instanceInfo);
		}

		/**
		 * Set the fold attribute's value (defines test set).
		 */
		public RemoveWithStringValues setValues(Set<String> values) {
			this.values = values;
			return this;
		}

		/**
		 * Find the fold attribute within a dataset.
		 */
		private Attribute getAttribute(Instances data) {
			SingleIndex index = new SingleIndex(super.getAttributeIndex());
			index.setUpper(data.numAttributes() - 1);
			Attribute att = data.attribute(index.getIndex());
			if (att == null)
				throw new NoSuchElementException("attribute #"
						+ super.getAttributeIndex() + " does not exist");
			if (!att.isNominal() && !att.isString())
				throw new IllegalArgumentException("Attribute '" + att
						+ "' is not nominal");
			return att;
		}

		/**
		 * Helper method to represent an Attribute as a single-element vector.
		 */
		private int[] toValueIndexArray(Attribute attribute) {
			TIntArrayList out = new TIntArrayList();
			for (String value : values) {
				int valueIndex = attribute.indexOfValue(value);
				if (valueIndex < 0)
					throw new NoSuchElementException("no such value: '" + value
							+ "' in attribute '" + attribute.toString() + "'");
				out.add(valueIndex);
			}
			return out.toNativeArray();
		}

		/**
		 * Split and return test or train instances.
		 */
		Instances split(Instances data, boolean invert, boolean removeAttribute)
				throws Exception {
			super.setInvertSelection(invert);
			super.setNominalIndicesArr(toValueIndexArray(getAttribute(data)));

			final Filter filter;
			if (!removeAttribute)
				filter = this;
			else {
				Remove remove = new Remove();
				remove.setAttributeIndices(super.getAttributeIndex());
				MultiFilter mf = new MultiFilter();
				mf.setFilters(new Filter[] { this, remove });
				filter = mf;
			}
			filter.setInputFormat(data);
			return Filter.useFilter(data, filter);
		}
	}

	static class RemoveWithPrefix extends Remove {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		String prefix;
		boolean matchClass;

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public void setMatchClass(boolean matchClass) {
			this.matchClass = matchClass;
		}

		@Override
		public boolean setInputFormat(Instances instanceInfo) throws Exception {
			TIntArrayList matchingIndices = new TIntArrayList();
			for (int i = 0; i < instanceInfo.numAttributes(); i++) {
				if (matchClass && i == instanceInfo.classIndex()) {
					matchingIndices.add(i);
					continue;
				}
				if (prefix != null
						&& instanceInfo.attribute(i).name().startsWith(prefix)) {
					matchingIndices.add(i);
					continue;
				}
			}
			super.setAttributeIndicesArray(matchingIndices.toNativeArray());
			return super.setInputFormat(instanceInfo);
		}
	}

	static class HackedClassifier extends SingleClassifierEnhancer {

		private static final long serialVersionUID = 1L;

		Set<String> sureClasses;

		public void setSureClasses(Set<String> sureClasses) {
			this.sureClasses = sureClasses;
		}

		@Override
		public void buildClassifier(Instances data) throws Exception {

			getClassifier().buildClassifier(data);
		}

		@Override
		public double[] distributionForInstance(Instance instance)
				throws Exception {
			double[] distribution = getClassifier().distributionForInstance(
					instance);
			int maxIndex = 0;
			for (int i = 0; i < distribution.length; i++) {
				if (distribution[maxIndex] < distribution[i])
					maxIndex = i;
			}
			final String maxLabel = instance.classAttribute().value(maxIndex);
			if (sureClasses.contains(maxLabel)) {
				Arrays.fill(distribution, 0.0);
				distribution[maxIndex] = 1.0;
				System.err.println("INFO: Hacked confidence of '" + maxLabel
						+ "'.");
			} else {
				Arrays.fill(distribution, 1.0d / instance.numClasses());
			}

			return distribution;
		}

	}
}
