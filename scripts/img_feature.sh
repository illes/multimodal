#!/bin/sh

HADOOP_HOME=/home/hadoop/hadoop-0.20.205.0
JARS=/home/hadoop/multimodal/lib/google-collections-1.0-rc2.jar,$HOME/multimodal/lib/mahout-math-0.5.jar,$HOME/multimodal/lib/mahout-core-0.5.jar,$HOME/multimodal/dist/iclef2011.jar
CONF=/home/hadoop/multimodal/conf/hadoop_wrapper.xml

$HADOOP_HOME/bin/hadoop pipes -libjars $JARS -conf $CONF -input $1 -output $2 -inputformat bme.iclef.hadoop.seq.ImageInputFormat -writer bme.iclef.hadoop.ImageOutputWriter -jobconf hadoop.pipes.java.recordwriter=true
