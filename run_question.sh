#!/usr/bin/env bash
IN_DIR=./data
OUT_DIR=./results
THREADS=${1-10}
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$IN_DIR $OUT_DIR $THREADS"
