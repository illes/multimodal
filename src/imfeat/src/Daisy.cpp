#include "Daisy.h"

#include <highgui.h>

#include "daisy/daisy.h"

Daisy::Daisy (int rad, int radq, int thq, int histq)
	: _rad (rad),
	_radq (radq),
	_thq  (thq),
	_histq (histq)
{
}

Daisy::~Daisy () {
}

bool Daisy::getFeatures (const char* fname, std::vector<std::vector<double> >& k) {
	bool ret = false;
	
	Ptr<Mat> img = readImgFromFile (fname, true);
	if (img == NULL) {
		return ret;
	}
	
	/* get the keypoints if there's a keypoint detector specified */
	vector<KeyPoint> kp;
	getKeypoints (*img, kp);

	/* get the daisy descriptors:
	 		- at keypoints if there's a keypoint detector given
			- all of them if there's no keypoint detector given
	 */
	ret = getDaisy (*img, k, kp);
	
	/* release the image */
	img.release ();
	return ret;
}

bool Daisy::getFeatures (const std::vector<char>& img, std::vector<std::vector<double> >& k) {
	bool ret = false;

	Ptr<Mat> src = readImgFromVector (img, true);
	if (src == NULL) {
		return ret;
	}
	
	/* get the keypoints if there's a keypoint detector specified */
	vector<KeyPoint> kp;
	getKeypoints (*src, kp);

	/* get the daisy descriptors:
	 		- at keypoints if there's a keypoint detector given
			- all of them if there's no keypoint detector given
	 */
	ret = getDaisy (*src, k, kp);

	/* release the image */
	src.release ();
	return ret;
}

bool Daisy::getDaisy (const Mat& img, std::vector<std::vector<double> >& k, const vector<KeyPoint>& kp) {
	/* create daisy extractor */
	daisy dy;

	/* set daisy parameters */
	dy.verbose (0);
	dy.set_parameters (_rad, _radq, _thq, _histq);

	/* set the new image */
 	dy.set_image (img.data, img.rows, img.cols);
  	dy.initialize_single_descriptor_mode ();

	/* calculate the descriptors */
  	dy.compute_descriptors ();
 	dy.normalize_descriptors ();

	if (!fd.empty ()) {
		/* if there's a keypoint detector, use it's output and get the descriptors
		   at keypoint locations
		 */
	  vector<KeyPoint>::const_iterator it = kp.begin (), end_it = kp.end ();
	  float* thor = new float[dy.descriptor_size ()];
	  if (thor == NULL)
		return false;
		
	  for ( ; it != end_it; ++it) {
	    KeyPoint kp = *it;
	    memset (thor, 0, sizeof(float)*dy.descriptor_size ());
	    /* do sanity checks for keypoints */
	    if ((kp.pt.y < 0) || (kp.pt.y > img.rows) || (kp.pt.x < 0) || (kp.pt.x > img.cols)) {
	      std::cout << "skipping invalid keypoint: " << kp.pt.x << " " << kp.pt.y << endl;
	      continue;
	    }

	    if ((kp.angle == 0) || (kp.angle >= 360)) {
	      std::cout << "skipping invalid orientation: " << kp.angle << endl;
	      continue;
	    }
			/* get the descriptor at the keypoint location */
	    if (kp.angle < 0) {
	      dy.get_descriptor (kp.pt.y, kp.pt.x, thor);
	    } else {
	      dy.get_descriptor (kp.pt.y, kp.pt.x, kp.angle, thor);
	    }
			
			/* copy the descriptor content to a vector */
			std::vector<double> descr;
	    for (int i = 0; i < dy.descriptor_size(); ++i) {
				descr.push_back (thor[i]);
	    }
		
			/* add descriptor to the descriptor vector */
			k.push_back (descr);
	  }		
		
	  if (kp.size() && kp[0].angle != -1)
    		delete [] thor;

	} else {
		/* no keypoint has been give, store all the descriptors */
		float* descr = dy.get_dense_descriptors ();
		for (int i = 0; i < img.cols; ++i) {
			for (int j = 0; j < img.rows; ++j) {
				std::vector<double> d;
				size_t pos = (i+j)*dy.descriptor_size();
				for (int l = 0; l < dy.descriptor_size(); ++l) {
					pos += l;
					d.push_back (descr[pos]);
		    }
				k.push_back (d);
			}
		}
	}
	
	/* reset, i.e. deallocate all memory by daisy */
	dy.reset ();

	return true;
}
