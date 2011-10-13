#include "histogram.h"

Histogram::Histogram (int hbins, int sbins, int vbins)
	: _hbins (hbins),
	_sbins (sbins),
	_vbins (vbins)
{

}

void Histogram::getHSV (const std::vector<char>& img, std::vector<double>& hv) const {
	Mat src;

	src = imdecode (Mat (img), 1);
	if (src.data == NULL) {
		/* problem with the decoding */
		return;
	}
	
	calcHSV (src, hv);
}

void Histogram::getHSV (const char* fname, std::vector<double>& hv) const {
	Mat src;

	src = imread (fname, 1);
	if (src.data == NULL) {
		/* problem with the decoding */
		return;
	}
	
	calcHSV (src, hv);
}

double Histogram::findMaxVal (const MatND& hist) const {
	double maxVal = -1;
	for (int v = 0; v < _vbins; v++)
		for( int h = 0; h < _hbins; h++ )
			for( int s = 0; s < _sbins; s++ ) {
				float val = hist.at<float>(h, s, v);
				if (val > maxVal) maxVal = val;
			}

	return maxVal;
}

void Histogram::calcHSV (const Mat& img, std::vector<double>& hv) const {
	if (img.data == NULL)
		return;

	int histSize[] = {_hbins, _sbins, _vbins};
	Mat hsv;
#if (CV_MAJOR_VERSION == 2) && (CV_MINOR_VERSION == 2)
	cvtColor (img, hsv, CV_BGR2HSV_FULL);
	// hue varies from 0 to 255
	float hranges[] = {0, 256};
#else
	cvtColor (img, hsv, CV_BGR2HSV);
	// hue varies from 0 to 175 
	float hranges[] = {0, 176};
#endif
	// saturation varies from 0 (black-gray-white) to
	// 255 (pure spectrum color)
	float sranges[] = {0, 256};
	// value varies from black to white
	float vranges[] = {0, 256};

	const float* ranges[] = {hranges, sranges, vranges};
	MatND hist;

	// we compute the histogram from the 0-th and 1-st channels
	int channels[] = {0, 1, 2};

	calcHist (&hsv, 1, channels, Mat(), // do not use mask
			hist, 3, histSize, ranges,
			true, // the histogram is uniform
			false);
	double maxVal = findMaxVal (hist);

	for (int v = 0; v < _vbins; v++) {
		for( int h = 0; h < _hbins; h++ ) {      
			for( int s = 0; s < _sbins; s++ ) {
				float binVal = hist.at<float>(h, s, v);
				hv.push_back (binVal/maxVal);
			}
		}
	}
}	

void Histogram::convertToCSV (const std::vector<double>& hv, std::string& s) const {
	std::stringstream ss;

	if (!hv.size ())
		return;

	for (size_t i = 0; i < hv.size (); ++i) {
		ss << hv[i];
	        if (i < (hv.size ()-1))
			ss << ",";	
	}
	s = ss.str ();
}
