package bme.iclef.mahout;

import java.util.Arrays;

import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;

/**
 * Provides a sorted iterator. 
 * 
 * Provides an alternative to calling
 * {@link SequentialAccessSparseVector#assign(Vector)}, but is more efficient,
 * as it does not copy the double values, and reuses it's internal array of
 * sorted indices, and sorts the indices using {@link Arrays#sort()} instead of
 * using insertion sort currently performed by
 * {@link SequentialAccessSparseVector#assign(Vector)}.
 * 
 * @author illes
 *
 */
public class ByIndexSparseVectorSorter extends SparseVectorSorter
{
	@Override
	protected void sort()
	{
		Arrays.sort(nonZeroIndices, 0, numNonZero);
	}

}
