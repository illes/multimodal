#ifndef __HISTOGRAM_H__
#define __HISTOGRAM_H__

#include <vector>

#include <cv.h>
#include <highgui.h>

#include "FeatureExtractor.h"

using namespace cv;

class Histogram : public FeatureExtractor {
		Histogram (Histogram&);
		Histogram& operator=(Histogram&);
	public:
		Histogram (int hbins = 10, int sbins = 10, int vbins = 1);
		
		virtual bool getFeatures (const char* fname, std::vector<std::vector<double> >& k) const;
		virtual bool getFeatures (const std::vector<char>& img, std::vector<std::vector<double> >& k) const;
		
		void convertToCSV (const std::vector<double>& hv, std::string& csv) const;

	private:
		double findMaxVal (const MatND& hist) const;
		void calcHSV (const Mat& img, std::vector<std::vector<double> >& k) const;
	private:
		int _hbins;
		int _sbins;
		int _vbins;
};

#endif

