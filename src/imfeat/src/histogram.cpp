#include <cv.h>
#include <highgui.h>
#include <iostream>
#include <stdio.h>
#include <getopt.h>
#include <libgen.h>

using namespace cv;

// let's quantize the hue to 10 levels
// and the saturation to 10 levels
static int hbins = 10, sbins = 10, vbins = 1;
static short csvFlag = 0;

static double
findMaxVal (const MatND& hist) {
  double maxVal = -1;
  for (int v = 0; v < vbins; v++)
     for( int h = 0; h < hbins; h++ )
         for( int s = 0; s < sbins; s++ ) {
           float val = hist.at<float>(h, s, v);
           if (val > maxVal) maxVal = val;
         }
        
  return maxVal;
}

static void
printCSVHeader () {
  assert (csvFlag);
  
  std::cout << "id,";
  for (int v = 0; v < vbins; v++) {
    if (v > 0)
      std::cout << ",";
    for( int h = 0; h < hbins; h++ ) {
      if (h > 0)
        std::cout << ",";
      for( int s = 0; s < sbins; s++ ) {
          if (s > 0)
            std::cout << ",";
          std::cout << "hist_" << h << "_" << s << "_" << v;
      }
    }
  }
  std::cout << std::endl;  
}

static void
generateHSVHist (char* file) {
  Mat src;
  char *fname = NULL;
  
  if (!(src=imread(file, 1)).data)
      return;
      
  /* get the supplied file name and strip the extension suffix */
  fname = strdup (basename (file));
  char* cc = strrchr (fname, '.');
  *cc = '\0';

  int histSize[] = {hbins, sbins, vbins};
  
  Mat hsv;
#if (CV_MAJOR_VERSION == 2) && (CV_MINOR_VERSION == 2)
  cvtColor (src, hsv, CV_BGR2HSV_FULL);
  // hue varies from 0 to 255
  float hranges[] = {0, 256};
#else
  cvtColor (src, hsv, CV_BGR2HSV);
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

  if (csvFlag) {
    std::cout << fname << ",";
  } else {
    std::cout << fname << std::endl;
  }
  
  
  for (int v = 0; v < vbins; v++) {
    for( int h = 0; h < hbins; h++ ) {      
        for( int s = 0; s < sbins; s++ )
        {
            float binVal = hist.at<float>(h, s, v);
            if (s > 0)
              std::cout << ",";
                          
            std::cout << (binVal/maxVal);
        }
        if (!csvFlag)
          std::cout << std::endl;
        else
          if (h != hbins-1)
            std::cout << ",";
    }
    if (csvFlag) {
      if (v != vbins-1)
        std::cout << ",";
    } else std::cout << std::endl;
  }
    
  if (csvFlag)
    std::cout << std::endl;
    
  free (fname);
}


int
main (int argc, char** argv) {  
  int c;
  opterr = 0;
  const char *file_list = NULL;

  while ((c = getopt (argc, argv, "b:cl:s:v:")) != -1) {
    switch (c) {
      case 'b':
        hbins = atoi (optarg);
        break;
      case 'l':
	file_list = optarg;
	break;
      case 's':
        sbins = atoi (optarg);
        break;
      case 'c':
        csvFlag = 1;
        break;
      case 'v':
        vbins = atoi (optarg);
        break;
      default:
        return -1;
    }    
  }

//  argc -= optind;
//  argv += optind;
  if (csvFlag)
    printCSVHeader ();
 
  if (file_list) {
	  char buf[255];
	  FILE *fd = fopen (file_list, "r");
	  if (!fd) {
		  printf ("erorr opening file list file\n");
		  exit (1);
	  }

	  while (fgets (buf, sizeof (buf), fd) != NULL) {
		  /* strip the trailing \n */
		  int len = strlen (buf)-1;
		  if (buf[len] == '\n')
			  buf[len] = '\0';
		  
		  generateHSVHist (buf);
	  }

	  fclose (fd);
  } else { 
	  for (int i = optind; i < argc; ++i) {
		  generateHSVHist (argv[i]);
	  }
  }

  return 0;
}
