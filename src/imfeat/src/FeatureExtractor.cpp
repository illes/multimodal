#include "FeatureExtractor.h"

#include <highgui.h>

FeatureExtractor::FeatureExtractor ()
	: fd (NULL)
{
	
}

FeatureExtractor::~FeatureExtractor () {
	fd.release ();
}

Ptr<Mat> FeatureExtractor::readImgFromFile (const char* fname, bool bw) const {
	Ptr<Mat> img;
	
	/* load the image. bw marks whether we want a b&w or RGB image */
	img = new Mat (imread (fname, (bw ? 0 : 1)));
	if (img->data == NULL) {
		/* problem with the decoding */
		return NULL;
	}
	
	img.addref ();
	
	return img;
}

Ptr<Mat> FeatureExtractor::readImgFromVector (const std::vector<char>& img, bool bw) const {
	Ptr<Mat> src;
	
	/* load the image. bw marks whether we want a b&w or RGB image */
	src = new Mat (imdecode (Mat (img), (bw ? 0 : 1)));
	if (src->data == NULL) {
		/* problem with the decoding */
		return NULL;
	}
	
	src.addref ();
		
	return src;
}

void FeatureExtractor::getKeypoints (const Mat& img, vector<KeyPoint>& kp) const {
	if (!fd.empty ()) {
		fd->detect (img, kp);
	}
}

void FeatureExtractor::setKeypointDetector (Ptr<FeatureDetector> KPDetector) {
	this->fd = KPDetector;
	fd.addref ();	
}

