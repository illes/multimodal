package bme.iclef.evaluate;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Compute precision, recall, F-measure for binary classification problems.
 * 
 * @author illes
 * 
 */
public class BinaryClassificationStatistics
{
	private static final double epsilon = 1e-6;

	public BinaryClassificationStatistics()
	{
		super();
	}

	public BinaryClassificationStatistics(int tp, int tn, int fp, int fn)
	{
		super();

		this.tp = tp;
		this.tn = tn;
		this.fp = fp;
		this.fn = fn;
	}

	int tp = 0;
	int tn = 0;
	int fp = 0;
	int fn = 0;

	final NumberFormat nf = new DecimalFormat("###0.000");

	/**
	 * 
	 * @param gold
	 * @param system
	 */
	public void addResult(boolean gold, boolean system)
	{
		if (gold)
		{
			if (system)
				++tp;
			else
				++fn;
		}
		else
		{
			if (system)
				++fp;
			else
				++tn;
		}
	}

	public double getPrecision()
	{
		return (tp + fp) > 0 ? (tp / ((double) tp + fp)) : 0;
	}

	public double getRecall()
	{
		return (tp + fn) > 0 ? (tp / ((double) tp + fn)) : 0;
	}

	public double getF()
	{
		double p = getPrecision();
		double r = getRecall();

		return ((p + r) > epsilon) ? ((2.0 * p * r) / (p + r)) : 0;
	}

	public String prettyPrint()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("       \t").append('N').append("\t").append('Y').append("\n");
		sb.append("True  N\t").append(String.format("%5d", tn)).append("\t").append(String.format("%5d", fp)).append("\n");
		sb.append("      Y\t").append(String.format("%5d", fn)).append("\t").append(String.format("%5d", tp)).append("\n\n");
		sb.append("P= ").append(nf.format(getPrecision())).append("\n");
		sb.append("R= ").append(nf.format(getRecall())).append("\n\n");
		sb.append("F= ").append(nf.format(getF())).append("\n");
		return sb.toString();
	}

	public void add(BinaryClassificationStatistics other)
	{
		this.tp += other.tp;
		this.tn += other.tn;
		this.fp += other.fp;
		this.fn += other.fn;
	}

	@Override
	public String toString()
	{
		return prettyPrint();
	}

}
