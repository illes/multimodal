#ifndef _VLDSIFT_H__
#define _VLDSIFT_H__
#include <string>
#include <vector>
#include <sstream>

#include <cv.h>
#include <highgui.h>

#include "vl/dsift.h"

#define VLDSIFT_DIM 128
using namespace cv;
using namespace std;

struct Point_vldsift {
  float x;
  float y;
  short scale;
  float norm;
  float desc[VLDSIFT_DIM];
};

struct Points_vldsift {
  vector<Point_vldsift> vecs;
  bool fromBinary(const string& bin ) {
    size_t len = bin.length();
    int num_points = len/sizeof(Point_vldsift);
    if( len % sizeof(Point_vldsift) != 0 ) {
      return false;
    }
    vecs.resize(num_points);
    memcpy(&vecs[0], bin.data(), num_points * sizeof(Point_vldsift));
    return true;
  }
  string toASCII() {
    ostringstream oss;
    oss << vecs.size() << " " << VLDSIFT_DIM << endl;
    for ( size_t i = 0; i< vecs.size(); i++ ) {
      Point_vldsift& p = vecs[i];
      oss << p.x << " " << p.y << " " << p.scale << " " << p.norm << " " << endl;
      for ( int k = 0; k < VLDSIFT_DIM; k++ ) {
	      oss << p.desc[k] << " ";
      }
      oss << endl;
    }
    return oss.str();
  }
  string toASCII_oneline() {
    ostringstream oss;
    for ( size_t i = 0; i< vecs.size(); i++ ) {
      Point_vldsift& p = vecs[i];
      oss << p.x << " " << p.y << " " << p.scale << " " << p.norm << " ";
      for ( int k = 0; k < VLDSIFT_DIM; k++ ) {
	oss << p.desc[k] << " ";
      }
    }
    return oss.str();
  }
  string toBinary() {
    string s;
    s.append((char*)&vecs[0], vecs.size()*sizeof(Point_vldsift));
    return s;
  }
};


class VLDSIFT {
		VLDSIFT (VLDSIFT&);
		VLDSIFT& operator=(VLDSIFT&);
		
	public:
		VLDSIFT (int step_size = 10) : _stepSize (step_size) {}
		
		void getDSIFT (const std::vector<char>& img, std::vector<std::vector<double> >& k) const {			
			/* load the image in grayscale */
			Mat src = imdecode (Mat (img), 0);
			if (src.data == NULL) {
				/* problem with the decoding */
				return;
			}
			
			calcDSIFT (src, k);
		}
		
		void getDSIFT (const char* fname, std::vector<std::vector<double> >& k) const {			
			/* load the image in grayscale */
			Mat src = imread (fname, 0);
			if (src.data == NULL) {
				/* problem with the decoding */
				return;
			}
			
			calcDSIFT (src, k);
		}
	
	private:
		void calcDSIFT (const Mat& img, std::vector<std::vector<double> >& k) const {
			/* convert the image into a float array */
			MatConstIterator_<float> it = img.begin<float>(), it_end = img.end<float>();
			float * fdata = (float*) malloc(img.cols * img.rows * sizeof (float)) ;
			for (int i = 0; it != it_end; it++, i++) {
				fdata[i] = *it;
			}
			
			VlDsiftFilter* df =  vl_dsift_new (img.cols, img.rows);
		  vl_dsift_set_steps (df, _stepSize, _stepSize);
		  vl_dsift_process (df, fdata);
		  int num_keypoints = vl_dsift_get_keypoint_num (df);

		  const float* descriptors = vl_dsift_get_descriptors (df);
		  int dim = vl_dsift_get_descriptor_size (df);
		
			for (int i = 0 ;i< num_keypoints; i++) {
				std::vector<double> descr;
		    for (int j = 0; j < dim; j++) {
					descr.push_back (descriptors[i*dim+j]);
		    }
				k.push_back (descr);
		  }
		
			vl_dsift_delete (df);
			delete [] fdata;
			
		}
		
	private:
		int _stepSize;
};

#endif
