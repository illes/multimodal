#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>
#include <getopt.h>
#include <libgen.h>

using namespace cv;
using namespace std;

#define DEBUG 0
#define THRESHOLD 5
#define RATIO 3.0
#define MARGIN 5
#define MEAN_THRESHOLD 200

static int nHor = 0;
static int nVer = 0;

void help()
{
	cout << "Find lines in an image with Hough Transform\n"
			"./houghlines <image>\n" << endl;
}

bool isVertical (const Vec4i& line) {
  if (abs(line[0]-line[2]) < THRESHOLD)
    return true;
  
  return false;
}

bool isHorizontal (const Vec4i& line) {
  if (abs(line[1]-line[3]) < THRESHOLD)
    return true;
  
  return false;
}

bool isGoodLine (const Size& imSize, const Vec4i& line) {
  double lineLength = 
    sqrt(pow((double)abs(line[0]-line[2]), 2.0) + pow((double)abs(line[1]-line[3]), 2.0));
  
  int bMargin = imSize.height - MARGIN;
  int rMargin = imSize.width - MARGIN;
  
  if 
  (
    isHorizontal (line) && 
    ((double)imSize.width)/lineLength < RATIO &&
    (line[1] > MARGIN && line[3] > MARGIN && line[1] < (bMargin) && line[3] < (bMargin))
  ) {
    nHor++;
    return true;
  } else if 
  (
    isVertical (line) && 
    ((double)imSize.height)/lineLength < RATIO &&
    (line[0] > MARGIN && line[2] > MARGIN && line[0] < (rMargin) && line[2] < (rMargin))    
  ) {
    nVer++;
    return true;
  }

  return false;
}

int main(int argc, char** argv)
{
    if (argc < 2) {
      help ();
      exit (1);
    }
    
    /* print header for the csv */
    cout << "id,is_gx,has_ver_and_hor" << endl;
    for (int i = optind; i < argc; ++i) {
      char* filename = argv[i];
    
      Mat src = imread (filename, 0);
      if(src.empty())
      {
          cout << "can not open " << filename << endl;
          exit (1);
      }
      Size imSize = src.size ();      
      Mat dst, cdst;
      Canny (src, dst, 10, 100, 3);
      cvtColor (dst, cdst, CV_GRAY2BGR);
      
      /* get base name of the file */
      char * fname = strdup (basename (filename));
      char* cc = strrchr (fname, '.');
      *cc = '\0';
      
      cout << fname << ",";
      
      free (fname);
      
      /* calculate mean for the image */
      Scalar imgMean = cv::mean (src);
      
      /* if mean is below threshold then it's neither a DR nor a GX, thus no line detection */
      if (imgMean[0] < MEAN_THRESHOLD) {
        cout << 0.0 << "," << 0 << endl;
        continue;
      }
      
      nHor = 0;
      nVer = 0;      
      vector<Vec4i> lines;
      HoughLinesP (dst, lines, 1, CV_PI/180, 150, 50, 5);
      double nGoodLines = 0.0;
      for (size_t i = 0; i < lines.size(); i++)
      {
          Vec4i l = lines[i];
          if (isGoodLine (imSize, l)) {
            nGoodLines += 1.0;
//              cout << l[0] << "," << l[1] << ";" << l[2] << "," << l[3] << endl; 
#if DEBUG        
              line (cdst, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(0,0,255), 1, CV_AA);
#endif
          }
      }
      
      if (lines.size())
        cout << (nGoodLines/(double)lines.size());
      else
        cout << 0;
      cout << "," << ( (nHor >= 1 && nVer >= 1) ? 1 : 0);  
      cout << endl;
#if DEBUG
      imshow ("source", src);
      imshow ("detected lines", cdst);

      waitKey();
//      cvDestroyWindow ("source");
//      cvDestroyWindow ("detected lines");

#if DETECT_CORNER      
      vector<Point2f> corners;
      int maxCorners = 20;
      double minDist = 10.0;
      goodFeaturesToTrack (dst, corners, maxCorners, 0.1, minDist);
      for (size_t i = 0; i < corners.size (); ++i) {
        Point2f corner = corners[i];
        circle (cdst, corner, 3, Scalar(255,0,0));
      }
      imshow ("detected lines", cdst);

      waitKey();
#endif /* DETECT_CORNER */
      
#endif /* DEBUG */
      
    }
    return 0;
}

