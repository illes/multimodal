#!/usr/bin/env python

import os
import sys

infile = sys.argv[1]
train_set_file = sys.argv[2]
test_set_file = sys.argv[3]
class_map_file = sys.argv[4]

def readSet (filename):
	fd = open (filename)
	idList = []
	for l in fd:
		idList.append (l.strip ())
	fd.close ()
	inSet = set (idList)
	return inSet

def readClassMap (filename):
	fd = open (filename)
	classMap = {}
	for l in fd:
		l = l.partition (" ")
		classMap[l[0]] = l[2].strip ()
	fd.close ()
	return classMap
	
def CSV2SMLR (filename, trainSet, testSet, classMap):
	fd = open (filename)
	ftrain = open (filename + ".train.smlr", "w")
	ftest = open (filename + ".test.smlr", "w")
	for l in fd:
		l = l.replace ("\"","")
		split = l.partition (",")
		vector = split[2].replace (","," ")
		classSplit = vector.rpartition (" ")
		classTxt = classSplit[2].strip ()
	 	if classTxt in classMap:
			vector = vector.replace (classTxt, classMap[classTxt])
		if split[0] in trainSet:
			ftrain.write (vector)
		elif split[0] in testSet:
			ftest.write (vector)
	fd.close ()
	ftrain.close ()
	ftest.close ()

trainSet = readSet (train_set_file)
testSet = readSet (test_set_file)

classMap = readClassMap (class_map_file)

CSV2SMLR (infile, trainSet, testSet, classMap)
