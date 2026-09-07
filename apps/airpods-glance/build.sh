#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$PROJECT_DIR/../../tools/android-env.sh"
android_resolve_tools 36.0.0 36
BUILD_DIR="$PROJECT_DIR/build/android"
CLASSES_DIR="$BUILD_DIR/classes"
DEX_DIR="$BUILD_DIR/dex"
LOCAL_DIR="$PROJECT_DIR/.local"
DEPS_DIR="$PROJECT_DIR/deps"
HIDDEN_API_VERSION=6.1
HIDDEN_API_AAR="$DEPS_DIR/hiddenapibypass-$HIDDEN_API_VERSION.aar"
HIDDEN_API_JAR="$DEPS_DIR/hiddenapibypass-$HIDDEN_API_VERSION.jar"
HIDDEN_API_URL="https://repo1.maven.org/maven2/org/lsposed/hiddenapibypass/hiddenapibypass/$HIDDEN_API_VERSION/hiddenapibypass-$HIDDEN_API_VERSION.aar"
HIDDEN_API_SHA256=e3161dd21c97a4540b1698a33f7062aeaa1450008e1e2176070e5380f7a6324c

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$DEX_DIR" "$DEPS_DIR"

if [ ! -f "$HIDDEN_API_AAR" ]; then
  curl -fsSL "$HIDDEN_API_URL" -o "$HIDDEN_API_AAR"
fi
printf '%s  %s\n' "$HIDDEN_API_SHA256" "$HIDDEN_API_AAR" | shasum -a 256 -c -
unzip -p "$HIDDEN_API_AAR" classes.jar > "$HIDDEN_API_JAR"

"$ANDROID_BUILD_TOOLS/aapt2" compile \
  --dir "$PROJECT_DIR/res" \
  -o "$BUILD_DIR/resources.zip"

"$ANDROID_BUILD_TOOLS/aapt2" link \
  -o "$BUILD_DIR/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$PROJECT_DIR/AndroidManifest.xml" \
  --java "$BUILD_DIR/generated" \
  --min-sdk-version 31 \
  --target-sdk-version 36 \
  "$BUILD_DIR/resources.zip"

find "$PROJECT_DIR/src" "$BUILD_DIR/generated" -name '*.java' -print0 \
  | xargs -0 javac \
      -encoding UTF-8 \
      -source 8 \
      -target 8 \
      -classpath "$ANDROID_JAR:$HIDDEN_API_JAR" \
      -d "$CLASSES_DIR"

(cd "$CLASSES_DIR" && jar cf "$BUILD_DIR/app-classes.jar" .)
"$ANDROID_BUILD_TOOLS/d8" \
  --lib "$ANDROID_JAR" \
  --min-api 31 \
  --output "$DEX_DIR" \
  "$BUILD_DIR/app-classes.jar" "$HIDDEN_API_JAR"

(cd "$DEX_DIR" && zip -q -u "$BUILD_DIR/app-unsigned.apk" classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 \
  "$BUILD_DIR/app-unsigned.apk" \
  "$BUILD_DIR/app-aligned.apk"

android_sign_debug \
  "$BUILD_DIR/app-aligned.apk" \
  "$BUILD_DIR/airpods-glance.apk" \
  airpods-glance-debug \
  "$LOCAL_DIR"
printf '%s\n' "$BUILD_DIR/airpods-glance.apk"
