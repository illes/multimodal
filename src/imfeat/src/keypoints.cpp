#include "cv.h"
#include "highgui.h"
#include <iostream>
#include <fstream>
#include <stdio.h>
#include <getopt.h>
#include <libgen.h>
#include <parallel/algorithm>

#include "daisy/daisy.h"

using namespace cv;
using namespace std;


static void
createCSVHeader (const char* fExtrType) {
  cout << "id,";
  size_t descrSize = 128;
  
  for (size_t i = 0; i < descrSize; ++i) {
    if (i > 0) cout << ",";
    cout << "feature_" << i;
  }
  cout << endl;
}

static void 
generateKeypoints (char* filename, Mat& img, vector<KeyPoint>& kp, const char* featureDetector) {
  cout << endl << "processing " << filename << endl;
  
  img = imread (filename, 0);

  /* open file */
  if (img.data == NULL || img.rows == 0 || img.cols == 0)
     return;

  
  /* get keypoints */
  if (!strcmp (featureDetector, "mser")) {
    Ptr<FeatureDetector> mser = FeatureDetector::create ("MSER");
    assert (mser != NULL);
    mser->detect (img, kp);
  } else if (!strcmp (featureDetector, "yape")) {
    LDetector yape;
    yape (img, kp);
  } else if (!strcmp (featureDetector, "orb")) {
    ORB orb;
    orb (img, Mat (), kp);
  }
}

static void 
generateDescriptorsAtKeypoints (char* filename, const Mat& img, vector<KeyPoint>& kp, const char* fExtrType) {
  char *fname = NULL;
  
  if (img.data == NULL || img.rows == 0 || img.cols == 0)
     return;

  /* get the supplied file name and strip the extension suffix */
  fname = strdup (basename (filename));
  char* cc = strrchr (fname, '.');
  *cc = '\0';
  string descrFileName (fname);
  
  /* get the descriptors at the mser locations */
  if (!strcmp (fExtrType, "sift")) {
    descrFileName += ".sift";
    ofstream outFile (descrFileName.c_str ());

    SIFT sift;
    Mat descr;
    sift (img, Mat (), kp, descr, true);

    for (int i = 0; i < descr.rows; ++i) {
      for (int j = 0; j < descr.cols; ++j) {
        if (j > 0) outFile << ",";
        outFile << descr.at<double> (i,j);
      }
      outFile << endl;
    }
  
    outFile.close ();
  } else if (!strcmp (fExtrType, "daisy")) {
    descrFileName += ".daisy";
    ofstream outFile (descrFileName.c_str ());
    
    int rad   = 15;
    int radq  =  3;
    int thq   =  8;
    int histq =  8;

    daisy* dy = new daisy();
    dy->verbose (0);
    dy->set_image (img.data, img.rows, img.cols);
    dy->set_parameters (rad, radq, thq, histq);
    dy->initialize_single_descriptor_mode ();

    dy->compute_descriptors ();
    dy->normalize_descriptors ();

    vector<KeyPoint>::const_iterator it = kp.begin ();
    float* thor = new float[dy->descriptor_size ()];
    for ( ; it != kp.end (); ++it) {
      KeyPoint kp = *it;
      memset (thor, 0, sizeof(float)*dy->descriptor_size ());
      /* do sanity checks for keypoints */
      if ((kp.pt.y < 0) || (kp.pt.y > img.rows) || (kp.pt.x < 0) || (kp.pt.x > img.cols)) {
	      cout << "skipping invalid keypoint: " << kp.pt.x << " " << kp.pt.y << endl;
	      continue;
      }

      if ((kp.angle == 0) || (kp.angle >= 360)) {
	      cout << "skipping invalid orientation: " << kp.angle << endl;
	      continue;
      }

      if (kp.angle < 0) {
        dy->get_descriptor (kp.pt.y, kp.pt.x, thor);
      } else {
        dy->get_descriptor (kp.pt.y, kp.pt.x, kp.angle, thor);
      }
      for (int i = 0; i < dy->descriptor_size(); ++i) {
        if (i > 0) outFile << ",";
        outFile << thor[i];
      }
      outFile << endl;
    }

    outFile.close ();

    delete dy;
    if (kp[0].angle != -1)
    	delete[] thor;

  } else if (!strcmp (fExtrType, "orb")) {
    descrFileName += ".orb";
    ofstream outFile (descrFileName.c_str ());
    
    ORB orb;
    Mat descr;
    orb (img, Mat (), kp, descr, true);
    for (int i = 0; i < descr.rows; ++i) {
      for (int j = 0; j < descr.cols; ++j) {
        if (j > 0) outFile << ",";
        outFile << descr.at<double> (i,j);
      }
      outFile << endl;
    }
    
    outFile.close ();
  } else {
    std::cout << "no such descriptor" << std::endl;
  }
  
  free (fname);
}

static char *descr = NULL;
static char *featureDetector = NULL;

void 
processFile (string fname) {
  Mat img;
  vector<KeyPoint> kp;
  
  generateKeypoints (const_cast<char*> (fname.c_str ()), img, kp, featureDetector);
  if (!kp.size ()) {
	  cout << "warning: no keypoints found in " << fname.c_str () << endl;
	  return;
  }
  
  generateDescriptorsAtKeypoints (const_cast<char*> (fname.c_str ()), img, kp, descr);  
}

int
main (int argc, char** argv) {
  int c;
  opterr = 0;
  char *list = NULL;

  while ((c = getopt (argc, argv, "f:d:l:")) != -1) {
    switch (c) {
      case 'f':
        featureDetector = optarg;
        break;
      case 'd':
        descr = optarg;
        break;
      case 'l':
        list = optarg;
        break;
      default:
        return -1;
    }    
  }
  
  if (list) {
    vector<string> fileList;
    char buf[255];
	  FILE *fd = fopen (list, "r");
	  if (!fd) {
		  printf ("erorr opening file list file\n");
		  exit (1);
	  }

	  while (fgets (buf, sizeof (buf), fd) != NULL) {
		  /* strip the trailing \n */
		  int len = strlen (buf)-1;
		  if (buf[len] == '\n')
			  buf[len] = '\0';
      string fname (buf);
      fileList.push_back (fname);
	  }

	  fclose (fd); 
	  
    std::for_each	(fileList.begin (), fileList.end (), processFile);
    //__gnu_parallel::for_each	(fileList.begin (), fileList.end (), processFile);
    fileList.clear ();
	  
  } else {
    for (int i = optind; i < argc; ++i) {
      processFile (string (argv[i]));
    }
  }
}
