#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT_DIR="$PROJECT_DIR/build/core-tests"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac -encoding UTF-8 -source 8 -target 8 -d "$OUT_DIR" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/BatteryComponent.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/UserOptions.java" \
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
  "$PROJECT_DIR/src/com/chocho/airpodsglance/WidgetVariant.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/WidgetLayout.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/WidgetSizing.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/WidgetState.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/LockWidgetState.java" \
  "$PROJECT_DIR/src/com/chocho/airpodsglance/LockWidgetSelection.java" \
  "$PROJECT_DIR/tests/com/chocho/airpodsglance/LockWidgetTests.java" \
  "$PROJECT_DIR/tests/com/chocho/airpodsglance/CoreTests.java" \
  "$PROJECT_DIR/tests/com/chocho/airpodsglance/WidgetTests.java" \
  "$PROJECT_DIR/tests/com/chocho/airpodsglance/WidgetVariantTests.java" \
  "$PROJECT_DIR/tests/com/chocho/airpodsglance/WidgetSizingTests.java"

java -cp "$OUT_DIR" com.chocho.airpodsglance.CoreTests
java -cp "$OUT_DIR" com.chocho.airpodsglance.WidgetTests
java -cp "$OUT_DIR" com.chocho.airpodsglance.WidgetVariantTests
java -cp "$OUT_DIR" com.chocho.airpodsglance.WidgetSizingTests
java -cp "$OUT_DIR" com.chocho.airpodsglance.LockWidgetTests
