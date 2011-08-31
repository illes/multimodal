#include <cv.h>
#include <highgui.h>
#include <iostream>
#include <ctype.h>
#include <getopt.h>
#include <libgen.h>

#define DEBUG 0 

void GetSkinMask(IplImage * src_RGB, IplImage * mask_BW, int erosions=1, int dilations=7);

int main (int argc, char *argv[]) {
    IplImage *pImage, *mask_BW;

    /* Get input image file name from command line */
    if ( argc < 2 ) {
	std::cerr << "usage: " << argv[0] << " <imagefile>" << std::endl;
        exit( -1 );
    }
    
    std::cout << "id,skin_mean" << std::endl;
    
    for (int i = optind; i < argc; ++i) {
      char *fname = NULL;
      pImage = cvLoadImage (argv[i], -1 );
      fname = strdup (basename (argv[i]));
      assert (fname != NULL);
      char* cc = strrchr (fname, '.');
      *cc = '\0';

      std::cout << fname << ",";
      
      if (pImage->nChannels == 3) {
        mask_BW = cvCreateImage (cvGetSize(pImage), IPL_DEPTH_8U, 3);
        GetSkinMask (pImage, mask_BW);      
        cv::Scalar mean = cv::mean (mask_BW);
//        for (int i = 0; i < 3; ++i) {
//          if (i > 0) std::cout << ","; 
          std::cout << mean[0]/255; 
//        }
           
#if DEBUG    
        /* Display the image and wait for user to press a key */
        cvNamedWindow( "Testing OpenCV...", 1 );

        while( 1 ) {
           cvShowImage( "Testing OpenCV...", pImage );
           if ( tolower( cvWaitKey( 0 )) == 'q' ) break;
           cvShowImage( "Testing OpenCV...", mask_BW );
           if ( tolower( cvWaitKey( 0 )) == 'q' ) break;
        }

        /* Clean up */
        cvDestroyWindow( "Testing OpenCV..." );
#endif

        cvReleaseImage( &mask_BW );
      } else {
//        for (int i = 0; i < 3; ++i) {
//          if (i > 0) std::cout << ","; 
          std::cout << 0; 
//        }
      }
      std::cout << std::endl;
      cvReleaseImage( &pImage );
      free (fname);
    }    

  return 0;
}

/* Detect the skin pixels within an image */
void GetSkinMask(IplImage * src_RGB, IplImage * mask_BW, int erosions, int dilations)
{
  CvSize size;

  CvSize sz = cvSize (src_RGB->width & -2, src_RGB->height & -2);
  //get the size of input_image (src_RGB)

  IplImage* pyr = cvCreateImage (cvSize(sz.width/2, sz.height/2), 8,  3 ); //create 2 temp-images

  IplImage* src = cvCreateImage (cvGetSize(src_RGB), IPL_DEPTH_8U,  3);
  cvCopyImage(src_RGB, src);

  IplImage* tmpYCR = cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 3);


  cvPyrDown( src, pyr, 7 );
  //remove noise from input
  cvPyrUp( pyr, src, 7 );

  cvCvtColor(src ,tmpYCR , CV_RGB2YCrCb);


  uchar Y;
  uchar Cr;
  uchar Cb;


  CvPixelPosition8u pos_src;
  CvPixelPosition8u pos_dst;

  int x =0;
  int y =0;

  CV_INIT_PIXEL_POS (pos_src, (unsigned char *) tmpYCR->imageData, 
                    tmpYCR->widthStep,  cvGetSize(tmpYCR),
                    x,y,
                    tmpYCR->origin);

  CV_INIT_PIXEL_POS(pos_dst,
                    (unsigned char *) mask_BW->imageData,
                    mask_BW->widthStep,
                    cvGetSize(mask_BW),
                    x,y,
                    mask_BW->origin);

  uchar * ptr_src;
  uchar * ptr_dst;


  for( y=0;y<src-> height; y++)
  {

    for ( x=0; x<src->width; x++)
    {

      ptr_src = CV_MOVE_TO(pos_src,x,y,3);
      ptr_dst = CV_MOVE_TO(pos_dst,x,y,3);

      Y = ptr_src[0];
      Cb= ptr_src[1];
      Cr= ptr_src[2];

      if 
      ( 
        Cr > 138 && Cr < 178 &&
        Cb + 0.6 * Cr > 200 && Cb + 0.6 * Cr <215
      )
      {
        ptr_dst[0] = 255;
        ptr_dst[1] = 255;
        ptr_dst[2] = 255;
      }
      else
      {
        ptr_dst[0] = 0;
        ptr_dst[1] = 0;
        ptr_dst[2] = 0;
      }
    }
  }

  if(erosions>0) cvErode(mask_BW,mask_BW,0,erosions);
  if (dilations>0) cvDilate(mask_BW,mask_BW,0,dilations);

  cvReleaseImage(&pyr);
  cvReleaseImage(&tmpYCR);
  cvReleaseImage(&src);
}
