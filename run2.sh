#!/usr/bin/env bash
IN_DIR=../wiki-coref/2
OUT_DIR=../results
THREADS=${1-100}
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$IN_DIR $OUT_DIR $THREADS"