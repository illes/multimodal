package bme.iclef.mahout;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;

/**
 * Provides a sorted iterator to be used as a wrapper around the unsorted
 * {@link RandomAccessSparseVector#iterateNonZero()}. 
 * 
 * @author illes
 * 
 */
public abstract class SparseVectorSorter
{
	Vector vector = null;
	int[] nonZeroIndices = null;
	int numNonZero = -1;

	int currentIndex = 0;

	final VectorIterator iterator = new VectorIterator();

	public SparseVectorSorter()
	{
		this(1024);
	}

	public SparseVectorSorter(int initialCapacity)
	{
		nonZeroIndices = new int[initialCapacity];
	}

	public Vector vector()
	{
		return vector;
	}

	public Iterator<Element> reset(Vector vector)
	{
		this.vector = vector;
		numNonZero = vector.getNumNondefaultElements();

		// reallocate if necessary
		if (nonZeroIndices.length < numNonZero)
			nonZeroIndices = new int[numNonZero * 2];

		int i = 0;
		for (Iterator<Element> iter = vector.iterateNonZero(); iter.hasNext();)
		{
			Element el = iter.next();
			nonZeroIndices[i++] = el.index();
		}
		sort();

		return rewind();
	}

	protected abstract void sort();

	public Iterator<Element> rewind()
	{
		if (vector == null)
			throw new IllegalStateException("no vector");
		currentIndex = 0;
		return iterator;
	}

	public void clear()
	{
		vector = null;
	}

	private class VectorIterator implements Iterator<Element>
	{

		@Override
		public boolean hasNext()
		{
			return currentIndex < numNonZero;
		}

		@Override
		public Element next()
		{
			if (!hasNext())
				throw new NoSuchElementException();

			return vector.getElement(nonZeroIndices[currentIndex++]);
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
