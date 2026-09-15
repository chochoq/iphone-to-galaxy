#!/bin/sh
set -eu
RENDER_TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RENDER_APP_DIR=$(CDPATH= cd -- "$RENDER_TEST_DIR/../.." && pwd)
RENDER_REPO_DIR=$(CDPATH= cd -- "$RENDER_APP_DIR/../.." && pwd)
. "$RENDER_REPO_DIR/tools/android-env.sh"
android_resolve_tools 36.0.0 36
if [ "$#" -ne 1 ] || [ -e "$1" ]; then
  printf '%s\n' 'Pass one fresh output directory (must not exist).' >&2
  exit 1
fi
mkdir -p "$1"
RENDER_OUT=$(CDPATH= cd -- "$1" && pwd)
RENDER_HIDDEN_JAR="$RENDER_APP_DIR/deps/hiddenapibypass-6.1.jar"
if [ ! -f "$RENDER_HIDDEN_JAR" ]; then
  printf '%s\n' 'Build the product app first to prepare its verified dependency.' >&2
  exit 1
fi
mkdir -p "$RENDER_OUT/classes" "$RENDER_OUT/dex" "$RENDER_OUT/generated"
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir "$RENDER_APP_DIR/res" -o "$RENDER_OUT/resources.zip"
"$ANDROID_BUILD_TOOLS/aapt2" link -o "$RENDER_OUT/unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$RENDER_TEST_DIR/AndroidManifest.xml" --java "$RENDER_OUT/generated" \
  --custom-package com.chocho.airpodsglance "$RENDER_OUT/resources.zip"
find "$RENDER_APP_DIR/src" "$RENDER_REPO_DIR/shared/android-ui/src" "$RENDER_TEST_DIR" "$RENDER_OUT/generated" -name '*.java' -print0 \
  | xargs -0 javac -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_JAR:$RENDER_HIDDEN_JAR" -d "$RENDER_OUT/classes"
(cd "$RENDER_OUT/classes" && jar cf "$RENDER_OUT/test.jar" .)
"$ANDROID_BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 31 --output "$RENDER_OUT/dex" "$RENDER_OUT/test.jar" "$RENDER_HIDDEN_JAR"
(cd "$RENDER_OUT/dex" && zip -q "$RENDER_OUT/unsigned.apk" classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" 4 "$RENDER_OUT/unsigned.apk" "$RENDER_OUT/aligned.apk"
android_sign_debug "$RENDER_OUT/aligned.apk" "$RENDER_OUT/test.apk" listening-widget-test "$RENDER_OUT/signing"
printf '%s\n' "$RENDER_OUT/test.apk"
