#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$PROJECT_DIR/../../tools/android-env.sh"
android_resolve_tools 36.0.0 36
BUILD_DIR="$PROJECT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
DEX_DIR="$BUILD_DIR/dex"
LOCAL_DIR="$PROJECT_DIR/.local"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$DEX_DIR"

"$ANDROID_BUILD_TOOLS/aapt2" compile \
  --dir "$PROJECT_DIR/res" \
  -o "$BUILD_DIR/resources.zip"

"$ANDROID_BUILD_TOOLS/aapt2" link \
  -o "$BUILD_DIR/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$PROJECT_DIR/AndroidManifest.xml" \
  --min-sdk-version 31 \
  --target-sdk-version 36 \
  "$BUILD_DIR/resources.zip"

find "$PROJECT_DIR/src" -name '*.java' -print0 \
  | xargs -0 javac \
      -encoding UTF-8 \
      -source 8 \
      -target 8 \
      -classpath "$ANDROID_JAR" \
      -d "$CLASSES_DIR"

find "$CLASSES_DIR" -name '*.class' -print0 \
  | xargs -0 "$ANDROID_BUILD_TOOLS/d8" \
      --lib "$ANDROID_JAR" \
      --min-api 31 \
      --output "$DEX_DIR"

(cd "$DEX_DIR" && zip -q -u "$BUILD_DIR/app-unsigned.apk" classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 \
  "$BUILD_DIR/app-unsigned.apk" \
  "$BUILD_DIR/app-aligned.apk"

android_sign_debug \
  "$BUILD_DIR/app-aligned.apk" \
  "$BUILD_DIR/tap-to-top.apk" \
  tap-to-top-debug \
  "$LOCAL_DIR"
printf '%s\n' "$BUILD_DIR/tap-to-top.apk"
