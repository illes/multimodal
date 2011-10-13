#include <iostream>
#include <stdio.h>
#include <getopt.h>
#include <libgen.h>

#include "histogram.h"

using namespace cv;


static int vbins = 10, hbins = 10, sbins = 1;
static short csvFlag = 0;

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
printCSVforFile (char* file, const std::string& hCSV) {
	char *fname = NULL;
	/* get the supplied file name and strip the extension suffix */
	fname = strdup (basename (file));
	char* cc = strrchr (fname, '.');
	*cc = '\0';

	std::cout << fname << "," << hCSV << std::endl;
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

  Histogram h (hbins, sbins, vbins);
  std::vector<double> hv;
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


		  h.getHSV (buf, hv);
		  
		  /* convert to CSV and print */
		  std::string hCSV;
		  h.convertToCSV (hv, hCSV);
		  printCSVforFile (buf, hCSV);

		  hv.clear ();
	  }

	  fclose (fd);
  } else { 
	  for (int i = optind; i < argc; ++i) {
		  h.getHSV (argv[i], hv);
		  std::string hCSV;
		  h.convertToCSV (hv, hCSV);
		  printCSVforFile (argv[i], hCSV);

		  hv.clear ();
	  }
  }

  return 0;
}
