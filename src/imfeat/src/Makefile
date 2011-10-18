SHELL = /bin/bash

Darwin_PPC_ARCH := mac
Darwin_Power_Macintosh_ARCH := mac
Darwin_i386_ARCH := maci
Linux_i386_ARCH := glx
Linux_i686_ARCH := glx
Linux_unknown_ARC := glx
Linux_x86_64_ARCH := a64

UNAME := $(shell uname -sm)
ARCH ?= $($(shell echo "$(UNAME)" | tr \  _)_ARCH)

# sanity check
ifeq ($(ARCH),)
die:=$(error $(err_no_arch))
endif

CC=g++
FLANN_CFLAGS=`pkg-config --cflags flann`
FLANN_LDFLAGS=`pkg-config --libs flann`
HDF5_CFLAGS=-I/opt/local/include
HDF5_LDFLAGS=-L/opt/local/lib -lhdf5
HADOOP_CFLAGS=-I$(HADOOP_HOME)/src/c++/libhdfs -I$(HADOOP_HOME)/src/c++/pipes/api

CFLAGS += -Wall -g
VL_CFLAGS=-I../vlfeat $(HDF5_CFLAGS) $(CFLAGS)
VL_LDFLAGS=../vlfeat/bin/$(ARCH)/libvl.a -lpthread
DAISY_CFLAGS=-I../daisy/include
DAISY_LDFLAGS=../daisy/lib/libdaisy.a
CV_CFLAGS=`pkg-config --cflags opencv` $(CFLAGS)
CV_LDFLAGS=`pkg-config --libs opencv`
SOURCES=vldsift.cpp clusterer.cpp histogram.cpp skin_detector.cpp houghlines.cpp keypoints.cpp
OBJECTS=$(SOURCES:.cpp=.o)
EXECUTABLE=vldsift cluster histogram detectsqr skin_detect houghlines keypoints

all: $(SOURCES) $(EXECUTABLE)

vldsift: vldsift.o
	$(CC) $(VL_CFLAGS) $< -o $@ $(VL_LDFLAGS)

cluster: clusterer.o
	$(CC) $(VL_CFLAGS) $< -o $@ $(VL_LDFLAGS) $(FLANN_LDFLAGS)

histogram: histogram.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

keypoints: keypoints.cpp
	$(CC) $(CV_CFLAGS) $(DAISY_CFLAGS) -fopenmp $< -o $@ $(CV_LDFLAGS) $(DAISY_LDFLAGS)
  
detectsqr: squares.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

skin_detect: skin_detector.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

houghlines: houghlines.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

hadoop_wrapper: hadoop_wrapper.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

.cpp.o:
	$(CC) $(CFLAGS) $(CV_CFLAGS) $(VL_CFLAGS) -c $<

clean:
	rm -rf $(OBJECTS) $(EXECUTABLE)
