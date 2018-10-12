#!/usr/bin/env bash
IN_DIR=../evidence/3
OUT_DIR=../results
THREADS=${1-30}
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$IN_DIR $OUT_DIR $THREADS"
