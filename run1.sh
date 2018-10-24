#!/usr/bin/env bash
IN_DIR=../evidence/1
OUT_DIR=../results
THREADS=${1-40}
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$IN_DIR $OUT_DIR $THREADS"
