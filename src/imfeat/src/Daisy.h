#ifndef __DAISY_H__
#define __DAISY_H__

#include "FeatureExtractor.h"
#include "daisy/daisy.h"

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

