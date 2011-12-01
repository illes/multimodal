#ifndef __FEATUREEXTRACTOR_H__
#define __FEATUREEXTRACTOR_H__

#include <cv.h>
#include <vector>

using namespace cv;

class FeatureExtractor {
	private:
		FeatureExtractor (FeatureExtractor&);
		FeatureExtractor& operator=(FeatureExtractor&);
	
	public:
		FeatureExtractor ();
		virtual ~FeatureExtractor ();
		
		virtual bool getFeatures (const char* fname, std::vector<std::vector<double> >& k) = 0;
		virtual bool getFeatures (const std::vector<char>& img, std::vector<std::vector<double> >& k) = 0;
		
		void setKeypointDetector (Ptr<FeatureDetector> KPDetector);

	protected:
		Ptr<Mat> readImgFromFile (const char* fname, bool bw = false) const;
		Ptr<Mat> readImgFromVector (const std::vector<char>& img, bool bw = false) const;
		void getKeypoints (const Mat& img, vector<KeyPoint>& kp) const;
		
	protected:
		Ptr<FeatureDetector> fd;
};


#endif
