package bme.iclef.predict;

import bme.iclef.predict.Prediction.Label;
import bme.iclef.representation.PMCArticle;
import bme.iclef.representation.PMCArticle.Figure;
import bme.iclef.representation.PMCArticle.Graphic;


public abstract class Predictor {
	
	/**
	 * Assign {@link Prediction}s to {@link Graphic} elements inside an {@link PMCArticle}.
	 * @note Subclasses cannot override this.
	 * @param article
	 */
	public final void predict(PMCArticle article) 
	{
		for (Figure f : article.figureList)
			predict(article, f);
	}

	/**
	 * Helper method to call {@link #predict(PMCArticle, Figure, Graphic)} for each {@link Graphic} inside the {@link Figure}.
	 * @note Subclasses may override this method.
	 * @param article
	 * @param f
	 */
	protected void predict(PMCArticle article, Figure f) {
		for (Graphic g : f.graphicList)
			predict(article, f, g);
	}

	/**
	 * Sub-classes should implement this method by adding {@link Prediction}s to {@link Graphic#predictionSet}. 
	 * @note Subclasses should implement this method.
	 * 
	 * @param article
	 * @param f
	 * @param g
	 */
	protected abstract void predict(PMCArticle article, Figure f, Graphic g);

	/**
	 * Helper method to add prediction to graphics. If a prediction with the same label exists, the confidence is set to the maximum of the two values.
	 * @return true, iff predictionSet has changed as a result of the call
	 */
	protected boolean overridePrediction(Graphic g, Label label, float confidence) {
		// missing or lower confidence?
		if (!g.predictionMap.containsKey(label) || g.predictionMap.get(label) < confidence)
		{
			g.predictionMap.put(label, confidence);
			return true;
		}
		// higher confidence was already set
		return false;
	}
}
