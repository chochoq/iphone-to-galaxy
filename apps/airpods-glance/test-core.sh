#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT_DIR="$PROJECT_DIR/build/core-tests"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac -encoding UTF-8 -source 8 -target 8 -d "$OUT_DIR" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/BatteryComponent.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/AirPodsPacketDecoder.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/AapBatteryDecoder.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/AapPlacementDecoder.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/AirPodsSnapshot.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/BatteryPresentation.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/LowBatteryPolicy.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/ConnectionPopupPolicy.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/ConnectionSheetPresentation.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/ProductMotionPolicy.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/ProductCompositionPolicy.java" \
  "$PROJECT_DIR/tests/com/chocho/airpodsglance/CoreTests.java"

java -cp "$OUT_DIR" com.chocho.airpodsglance.CoreTests
