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

HADOOP_HOME=/home/hadoop/hadoop-0.20.204.0
HADOOP_DIST=$(HADOOP_HOME)/c++/Linux-amd64-64
CV_HOME=/home/wiking/opencv2.2
JVM_HOME=/usr/lib/jvm/java-6-sun-1.6.0.26

CC=g++
FLANN_CFLAGS=`pkg-config --cflags flann`
FLANN_LDFLAGS=`pkg-config --libs flann`
HDF5_CFLAGS=-I/opt/local/include
HDF5_LDFLAGS=-L/opt/local/lib -lhdf5

HADOOP_CFLAGS=-I$(HADOOP_DIST)/include -I$(HADOOP_HOME)/src/c++/libhdfs
HADOOP_LDFLAGS=-L$(HADOOP_DIST)/lib -lhadooppipes -lhadooputils -lpthread -lnsl -lcrypto /home/hadoop/hadoop-0.21.0/c++/Linux-amd64-64/lib/libhdfs.a

JVM_CFLAGS=-I$(JVM_HOME)/include -I$(JVM_HOME)/include/linux
JVM_LDFLAGS=-L$(JVM_HOME)/jre/lib/amd64/server -ljvm

CFLAGS += -Wall -g
VL_CFLAGS=-I../vlfeat $(HDF5_CFLAGS) $(CFLAGS)
VL_LDFLAGS=../vlfeat/bin/$(ARCH)/libvl.a -lpthread
DAISY_CFLAGS=-I../daisy/include
DAISY_LDFLAGS=../daisy/lib/libdaisy.a
CV_CFLAGS=-I$(CV_HOME)/include -I$(CV_HOME)/include/opencv $(CFLAGS)
CV_LDFLAGS=-L$(CV_HOME)/lib -lopencv_highgui -lopencv_imgproc -lopencv_calib3d -lopencv_video -lopencv_core -lopencv_calib3d -lopencv_video /usr/lib/libjpeg.a /usr/lib/libpng12.a /usr/lib/libjasper.a /usr/lib/libtiff.a /usr/lib/libz.a -lrt
SOURCES=vldsift.cpp clusterer.cpp histogram.cpp skin_detector.cpp houghlines.cpp keypoints.cpp
OBJECTS=$(SOURCES:.cpp=.o)
EXECUTABLE=vldsift cluster histogram detectsqr skin_detect houghlines keypoints

all: $(SOURCES) $(EXECUTABLE)

vldsift: vldsift.o
	$(CC) $(VL_CFLAGS) $< -o $@ $(VL_LDFLAGS)

cluster: clusterer.o
	$(CC) $(VL_CFLAGS) $< -o $@ $(VL_LDFLAGS) $(FLANN_LDFLAGS)

histogram.o: histogram.cpp
	$(CC) $(CV_CFLAGS) -c $<

histogram: histogram_standalone.o histogram.o
	$(CC) $(CV_CFLAGS) $^ -o $@ $(CV_LDFLAGS)

keypoints: keypoints.cpp
	$(CC) $(CV_CFLAGS) $(DAISY_CFLAGS) -fopenmp $< -o $@ $(CV_LDFLAGS) $(DAISY_LDFLAGS)
  
detectsqr: squares.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

skin_detect: skin_detector.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

houghlines: houghlines.o
	$(CC) $(CV_CFLAGS) $< -o $@ $(CV_LDFLAGS)

hadoop_wrapper.o: hadoop_wrapper.cpp
	$(CC) $(JVM_CFLAGS) $(HADOOP_CFLAGS) $(CV_CFLAGS) -c $<

HDFSFile.o: HDFSFile.cpp
	$(CC) $(HADOOP_CFLAGS) $(JVM_CFLAGS) -c $<

hadoop_wrapper: hadoop_wrapper.o HDFSFile.o histogram.o
	$(CC) $^ -o $@ $(CV_LDFLAGS) $(HADOOP_LDFLAGS) $(JVM_LDFLAGS)

.cpp.o:
	$(CC) $(CFLAGS) $(CV_CFLAGS) $(VL_CFLAGS) -c $<

clean:
	rm -rf $(OBJECTS) $(EXECUTABLE)
