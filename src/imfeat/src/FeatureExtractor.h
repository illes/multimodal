#ifndef __FEATUREEXTRACTOR_H__
#define __FEATUREEXTRACTOR_H__

#include <cv.h>
#include <vector>

#include "daisy/daisy.h"

using namespace cv;

class FeatureExtractor {
	private:
		FeatureExtractor (FeatureExtractor&);
		FeatureExtractor& operator=(FeatureExtractor&);
	
	public:
		FeatureExtractor ();
		virtual ~FeatureExtractor ();
		
		virtual bool getFeatures (const char* fname, std::vector<std::vector<double> >& k) const = 0;
		virtual bool getFeatures (const std::vector<char>& img, std::vector<std::vector<double> >& k) const = 0;
		
		void setKeypointDetector (Ptr<FeatureDetector> KPDetector);

	protected:
		Ptr<Mat> readImgFromFile (const char* fname, bool bw = false) const;
		Ptr<Mat> readImgFromVector (const std::vector<char>& img, bool bw = false) const;
		void getKeypoints (const Mat& img, vector<KeyPoint>& kp) const;
		
	protected:
		Ptr<FeatureDetector> fd;
};


class Daisy : public FeatureExtractor {
	private: 
		Daisy (Daisy&);
		Daisy& operator=(Daisy&);

	public:
		Daisy (int rad = 15, int radq = 3, int thq = 8, int histq = 8);
		virtual ~Daisy ();
		
		virtual bool getFeatures (const char* fname, std::vector<std::vector<double> >& k);
		virtual bool getFeatures (const std::vector<char>& img, std::vector<std::vector<double> >& k);

	private:
		bool getDaisy (const Mat& img, std::vector<std::vector<double> >& k, const vector<KeyPoint>& kp);
		
	private:
		int _rad;
    int _radq;
    int _thq;
    int _histq;

		daisy dy;
};

#endif
