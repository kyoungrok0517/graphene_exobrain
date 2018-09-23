#!/usr/bin/env bash
IN_DIR=$1
OUT_DIR=$2
THREADS=${3:-10}
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$IN_DIR $OUT_DIR $THREADS"
