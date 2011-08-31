package bme.iclef.evaluate;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * Copied from:
 * package org.apache.mahout.classifier;
 * 
 * Modifications by Illes Solt.
 * 
 */


import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfusionMatrix
{

	private final Collection<String> labels;

	private final Map<String, Integer> labelMap;

	private final int[][] confusionMatrix;

	private final String defaultLabel; /*  = "unknown" */

	public ConfusionMatrix(Iterable<String> labels, String defaultLabel)
	{
		this.defaultLabel = defaultLabel;
		this.labelMap = new LinkedHashMap<String, Integer>();
		for (String label : labels)
		{
			labelMap.put(label, labelMap.size());
		}
		this.labels = labelMap.keySet();
		labelMap.put(defaultLabel, labelMap.size());
		this.confusionMatrix = new int[labelMap.size() + 1][labelMap.size() + 1];
	}

	public int[][] getConfusionMatrix()
	{
		return confusionMatrix;
	}

	public Collection<String> getLabels()
	{
		return labels;
	}

	public double getAccuracy(String label)
	{
		int labelId = labelMap.get(label);
		int labelTotal = 0;
		int correct = 0;
		for (int i = 0; i < labels.size(); i++)
		{
			labelTotal += confusionMatrix[labelId][i];
			if (i == labelId)
			{
				correct = confusionMatrix[labelId][i];
			}
		}
		return correct / (float) labelTotal;
	}
	
	public BinaryClassificationStatistics getStats(String label)
	{
		return getStats(labelMap.get(label));
	}

	private BinaryClassificationStatistics getStats(final int labelId)
	{
		int tp = confusionMatrix[labelId][labelId], fp=0, fn=0;
		for (int i = 0; i < labels.size(); i++)
		{
			 if (i!=labelId)
			 {
					fp += confusionMatrix[labelId][i];
					fn += confusionMatrix[i][labelId];
			 }
		}
		return new BinaryClassificationStatistics(tp, 0/*TODO*/, fp, fn);
	}

	public int getCorrect(String label)
	{
		int labelId = labelMap.get(label);
		return confusionMatrix[labelId][labelId];
	}
	
	public int getCorrect()
	{
		int sum = 0;
		for (String label : labels)
			sum += getCorrect(label);
		return sum;
	}

	public int getTotal()
	{
		int sum = 0;
		for (String label : labels)
			sum += getTotal(label);
		return sum;
	}
	
	public double getAccuracy()
	{
		int total = getTotal();
		if (total == 0)
			return Double.NaN;
		
		return getCorrect()/(double)total;
	}
	

	public double getTotal(String label)
	{
		int labelId = labelMap.get(label);
		int labelTotal = 0;
		for (int i = 0; i < labels.size(); i++)
		{
			labelTotal += confusionMatrix[labelId][i];
		}
		return labelTotal;
	}
	
	public double getAverageFMeasure()
	{
		double sum = 0;
		for (int i = 0; i < labels.size(); i++)
		{
			sum += getStats(i).getF();
		}
		return sum/labels.size();
	}
	

	public void addInstance(String correctLabel, String classifiedLabel)
	{
		increment(correctLabel, classifiedLabel);
	}

	public int getCount(String correctLabel, String classifiedLabel)
	{
		check(correctLabel);
		check(classifiedLabel);
		int correctId = labelMap.get(correctLabel);
		int classifiedId = labelMap.get(classifiedLabel);
		return confusionMatrix[correctId][classifiedId];
	}

	
	public void put(String correctLabel, String classifiedLabel, int count)
	{
		check(correctLabel);
		check(classifiedLabel);
		int correctId = labelMap.get(correctLabel);
		int classifiedId = labelMap.get(classifiedLabel);
		confusionMatrix[correctId][classifiedId] = count;
	}
	
	private void check(String label)
	{
		if (labels.contains(label) == false
				&& defaultLabel.equals(label) == false)
		{
			throw new IllegalArgumentException("Label '" + label + "'not found");
		}
	}

	public void increment(String correctLabel, String classifiedLabel, int count)
	{
		put(correctLabel, classifiedLabel, count
				+ getCount(correctLabel, classifiedLabel));
	}

	public void increment(String correctLabel, String classifiedLabel)
	{
		increment(correctLabel, classifiedLabel, 1);
	}

	public ConfusionMatrix merge(ConfusionMatrix b)
	{
		if (labels.size() != b.getLabels().size())
		{
			throw new IllegalArgumentException("The Labels do not Match");
		}

		// if (labels.containsAll(b.getLabels()))
		// ;
		for (String correctLabel : this.labels)
		{
			for (String classifiedLabel : this.labels)
			{
				increment(correctLabel, classifiedLabel, b.getCount(
						correctLabel, classifiedLabel));
			}
		}
		return this;
	}

	public String summarize()
	{
		StringBuilder returnString = new StringBuilder();
		
		returnString.append(
				"=======================================================")
				.append('\n');
		returnString.append("Confusion Matrix\n");
		returnString.append(
				"-------------------------------------------------------")
				.append('\n');

		for (String correctLabel : this.labels)
		{
			returnString.append(' ').append(getSmallLabel(labelMap.get(correctLabel)))
					.append('\t');
		}

		returnString.append("<--Classified as").append('\n');

		for (String correctLabel : this.labels)
		{
			int labelTotal = 0;
			for (String classifiedLabel : this.labels)
			{
				returnString.append(
						String.format("%5s", hyphenIfZero(getCount(correctLabel, classifiedLabel)))).append('\t');
				labelTotal += getCount(correctLabel, classifiedLabel);
			}
			returnString
					.append(" | ").append(String.format("%5s", hyphenIfZero(labelTotal)))
					.append('\t').append(String.format("%.02f", getStats(correctLabel).getF()))
					.append('\t').append(getSmallLabel(labelMap.get(correctLabel)))
					.append(" = ").append(correctLabel).append('\n');
		}
		returnString.append("Default Category: ").append(defaultLabel).append(": ").append(labelMap.get(defaultLabel)).append('\n');
		returnString.append("Avg F1:\t").append((float)getAverageFMeasure()).append('\n');
		returnString.append("Acc:\t").append((float)getAccuracy()).append('\n');
		returnString.append('\n');
		
		return returnString.toString();
	}
	
	static String hyphenIfZero(int n)
	{
		if (n == 0)
			return ".";
		else 
			return Integer.toString(n);
	}

	static String getSmallLabel(int i)
	{
		int val = i;
		StringBuilder returnString = new StringBuilder();
		do
		{
			int n = val % 26;
			int c = 'A';
			returnString.insert(0, (char) (c + n));
			val /= 26;
		} while (val > 0);
		return returnString.toString();
	}

}
