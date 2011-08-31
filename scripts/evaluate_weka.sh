#!/bin/sh


CLASSPATH=dist/*:lib/*
# uncomment to enable prifiling
#PROFILE="-javaagent:shiftone-jrat.jar"
JAVA="java ${PROFILE} -XX:+UseCompressedOops -XX:+UseParallelGC -Xmx4G"

#INPUT=data/articles.xml.out.xml
OUTPUT=$INPUT.evaluate.log
set -x
time $JAVA -cp "$CLASSPATH" 'bme.iclef.weka.Driver'  $@  2>&1 | tee "$OUTPUT"

