#!/usr/bin/env bash
IN_DIR=../evidence/4
OUT_DIR=../results
THREADS=${1-8}
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$IN_DIR $OUT_DIR $THREADS"
