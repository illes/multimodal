#include <iostream>
#include <cstdlib>
#include <cassert>
#include <fstream>

/*
extern "C" {
  #include "vl/kmeans.h"
}
*/

#include <flann/flann.hpp>
#include <flann/util/logger.h>
using namespace std;

static void
readASCIIvector (char* fname, flann::Matrix<float>& dataset) {
  ifstream ifs (fname, ifstream::in);
  size_t dim;
  size_t numData;
  
  ifs >> numData;
  assert (numData > 0);
  ifs >> dim;
  assert (dim > 0);
  cout << "num vect: " << numData << " dim: " << dim << endl;

  dataset = flann::Matrix<float> (new float[numData*dim], numData, dim);
  assert (dataset.data != NULL);
  
  int i=0;
  while (ifs.good ()) {
      for (size_t j=0; j < dim; ++j) {
        ifs >> dataset.data[dim*i+j];
      }
      i++;
  }
  
  ifs.close();
}



int
main (int argc, char** argv) {
  if (argc < 3) {
    cout << "Missing parameter!" << endl
        << "Usage: " << endl
        << "./cluster <feature vector file> <output index file>" << endl;
    return EXIT_FAILURE;
  }

  flann::logger.setLevel (FLANN_LOG_INFO);

  flann::Matrix<float> dataset;
  readASCIIvector (argv[1], dataset);
  flann::AutotunedIndex<flann::L2<float> > autoIndex (dataset);
  
  autoIndex.buildIndex ();

  FILE* fd = fopen (argv[2], "w+");
  autoIndex.saveIndex (fd);
  fclose (fd);
  
  /*
  vl_size numClusters = 10;
  vl_size dataDim = 0, numData = 0;
  VlKMeansAlgorithm algorithm = VlKMeansLLoyd;
  VlVectorComparisonType distance = VlDistanceL2;
  VlKMeans * kmeans = NULL;
  float * data = NULL;
  
  
  kmeans = vl_kmeans_new (VL_TYPE_FLOAT, distance);
  assert (kmeans != NULL);

  vl_kmeans_set_initialization (kmeans, VlKMeansRandomSelection);
  vl_kmeans_set_algorithm (kmeans, algorithm);
  vl_kmeans_set_max_num_iterations (kmeans, 100);
  vl_kmeans_set_verbosity (kmeans, 2);
  
  for (int i = 1; i < argc; ++i) {
    readASCIIvector (argv[i], &numData, &dataDim, &data);
    assert (numData > 0);
    assert (dataDim > 0);
    assert (data != NULL);
  }
  
  vl_kmeans_cluster (kmeans, data, dataDim, numData, numClusters);
  
  vl_kmeans_delete (kmeans);
  free (data);
  */
  return EXIT_SUCCESS;
}