#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT_DIR="$PROJECT_DIR/build/core-tests"
mkdir -p "$OUT_DIR"

javac -encoding UTF-8 -source 8 -target 8 -d "$OUT_DIR" \
  "$PROJECT_DIR/src/com/chocho/ui/EditorModel.java" \
  "$PROJECT_DIR/src/com/chocho/ui/SaveJobs.java" \
  "$PROJECT_DIR/tests/EditorModelTests.java" \
  "$PROJECT_DIR/tests/SaveJobsTests.java"

java -cp "$OUT_DIR" com.chocho.ui.EditorModelTests
java -cp "$OUT_DIR" com.chocho.ui.SaveJobsTests
