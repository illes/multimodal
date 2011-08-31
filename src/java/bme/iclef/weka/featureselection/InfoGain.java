package bme.iclef.weka.featureselection;

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
 *    Adapted from: 
 *    InfoGainAttributeEval.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Instance;
import weka.core.Instances;
import bme.iclef.weka.CSVLoader;

/**
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Illes Solt
 */
public class InfoGain
{
	private static final Logger log = LoggerFactory.getLogger(InfoGain.class);

	/** for serialization */
	static final long serialVersionUID = -1949849512589218930L;

	private static final double EPSILON = 1e-6;

	/** Just binarize numeric attributes */
	private boolean m_Binarize = true;

	/** The info gain for each attribute */
	private double[] m_InfoGains;

	/**
	 * Binarize numeric attributes.
	 * 
	 * @param b
	 *            true=binarize numeric attributes
	 */
	public void setBinarizeNumericAttributes(boolean b)
	{
		m_Binarize = b;
	}

	/**
	 * get whether numeric attributes are just being binarized.
	 * 
	 * @return true if missing values are being distributed.
	 */
	public boolean getBinarizeNumericAttributes()
	{
		return m_Binarize;
	}

	public void buildEvaluator(Instances data)
			throws Exception
	{
		buildEvaluator(data, createInstanceFromClass(data, data.classIndex()));
	}

	/**
	 * Initializes an information gain attribute evaluator. Discretizes all
	 * attributes that are numeric.
	 * 
	 * @param data
	 *            set of instances serving as training data
	 */
	public void buildEvaluator(final Instances data, int[] classInstance)
	{
		if (data.numInstances() != classInstance.length)
			throw new RuntimeException("incomptaible sizes");

		if (!getBinarizeNumericAttributes())
			throw new UnsupportedOperationException("non-binary discretization not implemented");

		final int numInstances = data.numInstances();
		final int numClasses;
		final boolean hasMissing;
		{
			Set<Integer> classes = new HashSet<Integer>();
			boolean missing = false;
			for (int i = 0; i < classInstance.length; i++)
			{
				if (classInstance[i] != -1.0)
					classes.add(classInstance[i]);
				else
					missing = true;
			}
			numClasses = data.classAttribute().numValues();
			hasMissing = missing;
		}

		// Reserve space and initialize counters
		// counts: feature x value x class
		int[][][] counts = new int[data.numAttributes() + 1][][];//[2][numClasses + (hasMissing ? 1 : 0)];

		// Initialize counters
		final int[] temp;
		{
			temp = new int[numClasses + (hasMissing ? 1 : 0)];
			for (int k = 0; k < numInstances; k++)
			{
				final int classValueIndex = classInstance[k];
				if (classValueIndex == -1)
					temp[numClasses] += 1; // inst.weight();
				else
					temp[classValueIndex] += 1; // inst.weight();
			}
		}

		// Get counts
		for (int k = 0; k < numInstances; k++)
		{
			final int classValueIndex = classInstance[k];
			final Instance inst = data.instance(k);
			for (int i = 0; i < inst.numAttributes(); i++)
			{
				if (i == data.classIndex())
					continue;
				final int key = i + 1;
				if (counts[key] == null)
				{
					counts[key] = new int[2][numClasses + (hasMissing ? 1 : 0)];
					for (int j = 0; j < temp.length; j++)
						counts[key][0][j] = temp[j];
				}
//				if (key == 0)
//					throw new IllegalStateException("expected positive index");
				if (classValueIndex == -1)
				{
					counts[key][binarize(inst.value(i))][numClasses] += 1; // inst.weight();
					counts[key][0][numClasses] -= 1;
				}
				else
				{
					counts[key][binarize(inst.value(i))][classValueIndex] += 1; // inst.weight();
					counts[key][0][classValueIndex] -= 1; // inst.weight();
				}
			}
		}

		// Compute info gains
		m_InfoGains = new double[data.numAttributes()];
		for (int i = 0; i < data.numAttributes(); i++)
			if (counts[i] != null)
				m_InfoGains[i] = entropyOverColumns(counts[i])
								- entropyConditionedOnRows(counts[i]);
	}

	private static final int binarize(double value)
	{
		return value != 0.0 ? 1 : 0;
	}

	/**
	 * Reset options to their default values
	 */
	protected void resetOptions()
	{
		m_InfoGains = null;
		m_Binarize = false;
	}

	/**
	 * evaluates an individual attribute by measuring the amount of information
	 * gained about the class given the attribute.
	 * 
	 * @param attribute
	 *            the index of the attribute to be evaluated
	 * @return the info gain
	 * @throws Exception
	 *             if the attribute could not be evaluated
	 */
	public double evaluateAttribute(int attribute)
			throws Exception
	{
		return m_InfoGains[attribute];
	}

	/**
	 * Computes the columns' entropy for the given contingency table.
	 * 
	 * @param matrix
	 *            the contingency table
	 * @return the columns' entropy
	 */
	private static double entropyOverColumns(final int[][] matrix)
	{

		double returnValue = 0, sumForColumn, total = 0;

		for (int j = 0; j < matrix[0].length; j++)
		{
			sumForColumn = 0;
			for (int i = 0; i < matrix.length; i++)
			{
				sumForColumn += matrix[i][j];
			}
			returnValue = returnValue - lnFunc(sumForColumn);
			total += sumForColumn;
		}
		if (Math.abs(total) < EPSILON)
		{
			return 0;
		}
		return (returnValue + lnFunc(total)) / (total * Math.log(2));
	}

	/**
	 * Computes conditional entropy of the columns given the rows.
	 * 
	 * @param matrix
	 *            the contingency table
	 * @return the conditional entropy of the columns given the rows
	 */
	private static double entropyConditionedOnRows(int[][] matrix)
	{
		double returnValue = 0, sumForRow, total = 0;

		for (int i = 0; i < matrix.length; i++)
		{
			sumForRow = 0;
			for (int j = 0; j < matrix[0].length; j++)
			{
				returnValue = returnValue + lnFunc(matrix[i][j]);
				sumForRow += matrix[i][j];
			}
			returnValue = returnValue - lnFunc(sumForRow);
			total += sumForRow;
		}
		if (Math.abs(total) < EPSILON)
		{
			return 0;
		}
		return -returnValue / (total * Math.log(2));
	}

	/**
	 * Help method for computing entropy.
	 */
	private static final double lnFunc(double num)
	{
		// Constant hard coded for efficiency reasons
		if (num < 1e-6)
		{
			return 0;
		}
		else
		{
			return num * Math.log(num);
		}
	}

	public AttributeInfoGain[] topAttributes(final int n)
	{
		Queue<AttributeInfoGain> all = new PriorityQueue<AttributeInfoGain>(m_InfoGains.length,
				new Comparator<AttributeInfoGain>()
		{
			@Override
			public int compare(AttributeInfoGain o1, AttributeInfoGain o2)
			{
				return Double.compare(o2.infoGain, o1.infoGain); // descending
			}
		});
		for (int i = 0; i < m_InfoGains.length; i++)
			all.add(new AttributeInfoGain(i, m_InfoGains[i]));
		AttributeInfoGain[] best = new AttributeInfoGain[n];
		for (int i = 0; i < best.length; i++)
		{
			best[i] = all.remove();
		}
		return best;
	}

	// public AttributeInfoGain[] topAttributes(Dataset data, Instance
	// classInstance, int n)
	// {
	// buildEvaluator(data, classInstance);
	// return topAttributes(n);
	// }

	public static class AttributeInfoGain
	{
		public int index;
		public double infoGain;

		public AttributeInfoGain(int index, double infoGain)
		{
			this.index = index;
			this.infoGain = infoGain;
		}

	}
	
	protected static int[] createInstanceFromClass(Instances data, int classIndex)
	{
		final int[] classInstance = new int[data.numInstances()];
		for (int i = 0; i < classInstance.length; i++) {
			classInstance[i] = (int) data.instance(i).value(classIndex);
		}
		return classInstance;
		
	}
	

	public static class InfoGainFeatureSelection
	{
		Set<Integer> selectedAttrs;

		private boolean oneVsRest;

		private int n;
		
		/**
		 * @param n the number of features to select per class, iff <code>oneVsRest</code> is set, in total otherwise.
		 * @param oneVsRest whether to perform feature selection per class
		 */
		public InfoGainFeatureSelection(int n, boolean oneVsRest)
		{
			super();
			this.oneVsRest = oneVsRest;
			this.n = n;
		}

		public Set<Integer> selectedAttributes()
		{
			return selectedAttrs;
		}
		
		public String selectedAttributeRange()
		{
			return StringUtils.join(selectedAttributes().toArray(), ",");
		}


		public void build(Instances data)
		{
			try
			{
				selectedAttrs = new HashSet<Integer>();
				
				if (oneVsRest)
				{
					ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

					int[] classLabels = createInstanceFromClass(data, data.classIndex());
					
					// push tasks
					List<Future<AttributeInfoGain[]>> futures = new ArrayList<Future<AttributeInfoGain[]>>(
							data.classAttribute().numValues());
					for (int classValueIndex = 0; classValueIndex < data.classAttribute().numValues(); classValueIndex++)
						futures.add(es.submit(new OneVsRestTask(data, classValueIndex, toBinary(classLabels, classValueIndex))));

					// collect task results
					for (int classValueIndex = 0; classValueIndex < data.classAttribute().numValues(); classValueIndex++)
					{
						AttributeInfoGain[] aigs = futures.get(classValueIndex).get(); 
						select(aigs);
						for (AttributeInfoGain aig : aigs)
						{
							System.err.println("CLASS: " + data.classAttribute().value(classValueIndex) +"\tIG: " + (float)aig.infoGain +"\tATTR: " + data.attribute(aig.index));
						}
					}
					es.shutdown();
				}
				else
				{
					InfoGain ig = new InfoGain();
					ig.buildEvaluator(data);
					select(ig.topAttributes(n));
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		private void select(AttributeInfoGain[] attributeInfoGains)
		{
			for (AttributeInfoGain ag : attributeInfoGains)
				selectedAttrs.add(ag.index);
		}

		private class OneVsRestTask implements Callable<AttributeInfoGain[]>
		{
			final Instances data;
			final int[] classInstance;
			final int classValueIndex;

			public OneVsRestTask(Instances data, int classValueIndex, int[] classInstance)
			{
				this.data = data;
				this.classValueIndex = classValueIndex;
				this.classInstance = classInstance;
			}

			@Override
			public AttributeInfoGain[] call()
			{
				InfoGain ig = new InfoGain();
				final String className = data.classAttribute().value(classValueIndex);
				log.info("Running Feature selection for class {} '{}' ...", classValueIndex, className);
				// ig.buildEvaluator(data, oneVsRestClassInstance);
				ig.buildEvaluator(data, classInstance);
				log.info("Picking top attributes ...");
				AttributeInfoGain[] top = ig.topAttributes(n);
				return top;
			}
		}

		private static int[] toBinary(final int[] classInstance, final int oneClazzIndex)
		{
			int[] out = new int[classInstance.length];
			int count = 0;
			for (int j = 0; j < out.length; j++)
			{
				int classValueIndex = (int) classInstance[j];
				if (classValueIndex == -1)
					out[j] = -1;
				else
					out[j] = classValueIndex == oneClazzIndex ? 1 : 0;

				if (classValueIndex == oneClazzIndex)
					count++;
			}
			log.info("Class {} support: {}", oneClazzIndex, count);
			return out;
		}
		
		public static void main(String[] args) throws FileNotFoundException, IOException {
			CSVLoader csvLoader = new CSVLoader();
			csvLoader.setNominalAttributes("last");
			csvLoader.setSource(new FileInputStream(args[0]));

			Instances data = csvLoader.getDataSet();
			if (data.classIndex() < 0)
				data.setClassIndex(data.numAttributes()-1);
			data.deleteWithMissingClass();
			InfoGainFeatureSelection igfs = new InfoGainFeatureSelection(10, true);
			igfs.build(data);
			Set<Integer> selectedAttributes = new TreeSet<Integer>(igfs.selectedAttributes());
			for (int attrIndex : selectedAttributes)
			{
				System.err.println("ATTR: " + data.attribute(attrIndex));
			}
			System.err.println(Arrays.toString(selectedAttributes.toArray()));
		}

	}
}
