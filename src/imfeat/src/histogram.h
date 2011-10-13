#ifndef __HISTOGRAM_H__
#define __HISTOGRAM_H__

#include <vector>

#include <cv.h>
#include <highgui.h>

using namespace cv;

class Histogram {
		Histogram (Histogram&);
		Histogram& operator=(Histogram&);
	public:
		Histogram (int hbins = 10, int sbins = 10, int vbins = 1);
		
		void getHSV (const char* fname, std::vector<double>& hv) const;
		void getHSV (const std::vector<char>& img, std::vector<double>& hv) const; 

		void convertToCSV (const std::vector<double>& hv, std::string& csv) const;

	private:
		double findMaxVal (const MatND& hist) const;
		void calcHSV (const Mat& img, std::vector<double>& hv) const;
	private:
		int _hbins;
		int _sbins;
		int _vbins;
};

#endif

