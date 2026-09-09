#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT_DIR="$PROJECT_DIR/build/core-tests"
mkdir -p "$OUT_DIR"
javac -encoding UTF-8 -d "$OUT_DIR" \
  "$PROJECT_DIR/src/com/chocho/taptotop/ScrollOptions.java" \
  "$PROJECT_DIR/src/com/chocho/taptotop/TapTrigger.java" \
  "$PROJECT_DIR/tests/com/chocho/taptotop/OptionsTests.java"
java -cp "$OUT_DIR" com.chocho.taptotop.OptionsTests
