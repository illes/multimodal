#!/usr/bin/env python

import os
import sys
from convert_utils import *

def genSparseVector (id, vector):
	libSVMVector = "%s " % id
	for i in range (1, (len (vector)-1)):
		sparseVect = "%d:%s" % (i, vector[i])
		libSVMVector += sparseVect
		if i < (len (vector)-2):
			libSVMVector += " "
		else:
			libSVMVector += "\n"

	return libSVMVector
	
def CSV2LibSVM (filename, trainSet, testSet, classMap):
	fd = open (filename)
	ftrain = open (filename + ".train.libsvm", "w")
	ftest = open (filename + ".test.libsvm", "w")
	for l in fd:
		l = l.replace ("\"","")
		vector = l.split (",")
		featureId = vector[0]
		label = vector[len(vector)-1].strip ()
		labelId = -1
		if label in classMap:
			 labelId = classMap[label]
			
		if featureId in trainSet:
			ftrain.write (genSparseVector (labelId, vector))
		elif featureId in testSet:
			ftest.write (genSparseVector (labelId, vector))
					
	fd.close ()
	ftrain.close ()
	ftest.close ()

infile = sys.argv[1]
train_set_file = sys.argv[2]
test_set_file = sys.argv[3]
class_map_file = sys.argv[4]

trainSet = readSet (train_set_file)
testSet = readSet (test_set_file)
classMap = readClassMap (class_map_file)

CSV2LibSVM (infile, trainSet, testSet, classMap)
