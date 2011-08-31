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
 *    ClusterMembership.java
 *    Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package bme.iclef.weka;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.unsupervised.attribute.ClusterMembership;

/**
 * {@link ClusterMembership} that adds cluster membership probability attributes
 * to existing attributes.
 * 
 * @author illes
 * 
 */
public class AddClusterMembership extends ClusterMembership {

	private static final long serialVersionUID = 1L;

	protected Instance currentInstance;

	@Override
	protected void setOutputFormat(Instances outputFormat) {
		Instances out = new Instances(getInputFormat(), 0);

		// append the attributes
		for (int i = 0; i < outputFormat.numAttributes(); i++)
			if (outputFormat.classIndex() != i)
				out.insertAttributeAt(
						outputFormat.attribute(i), 
						getInputFormat().numAttributes() + i);

		super.setOutputFormat(out);
	}

	@Override
	protected void convertInstance(Instance instance) throws Exception {
		currentInstance = instance;
		super.convertInstance(instance);
	}
	
	@Override
	public boolean batchFinished() throws Exception {
		if (outputFormatPeek() == null)
			System.err.println("INFO: Building clusterer from " + getInputFormat().numInstances() +" instances...");
		return super.batchFinished();
	}

	@Override
	protected void push(Instance clusterInstance) {
		final Instance out;
		if (currentInstance instanceof SparseInstance)
		{
			out = new SparseInstance(outputFormatPeek().numAttributes());
			throw new IllegalStateException("not implemented"); // could use the same for(...) loop from below, but would be inefficient 
		}
		else
		{
			out = new Instance(outputFormatPeek().numAttributes());
			for (int i = 0; i < getInputFormat().numAttributes(); i++)
				out.setValue(i, currentInstance.value(i));
		}

		// append the attribute values
		for (int i = 0; i + getInputFormat().numAttributes() < outputFormatPeek().numAttributes(); i++)
			out.setValue(
					getInputFormat().numAttributes() + i,
					clusterInstance.value(i));

		super.push(out);
	}
}
