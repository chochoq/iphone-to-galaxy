#!/bin/sh
# Explicit local signing only. No key generation, installation or upload.
set -eu
if [ "$#" -ne 2 ]; then
  printf '%s\n' 'Usage: sh tools/sign-release.sh APP OUTPUT_APK' >&2
  exit 1
fi
case "$1" in
  airpods-glance) build=build/android ;;
  holiday-sleep|tap-to-top) build=build ;;
  *) printf '%s\n' 'Unknown app.' >&2; exit 1 ;;
esac
: "${APK_RELEASE_KEYSTORE:?Set a private keystore path outside the repository}"
: "${APK_RELEASE_ALIAS:?Set the key alias}"
: "${APK_RELEASE_PASSWORD:?Set the keystore password in the environment}"
REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
. "$REPO_DIR/tools/android-env.sh"
android_resolve_tools 36.0.0 36
input="$REPO_DIR/apps/$1/$build/app-aligned.apk"
if [ ! -f "$input" ] || [ ! -f "$APK_RELEASE_KEYSTORE" ]; then
  printf '%s\n' 'Build the app first and check the keystore path.' >&2
  exit 1
fi
if [ -e "$2" ]; then
  printf '%s\n' 'Output already exists; choose a new file.' >&2
  exit 1
fi
"$ANDROID_BUILD_TOOLS/apksigner" sign \
  --ks "$APK_RELEASE_KEYSTORE" --ks-key-alias "$APK_RELEASE_ALIAS" \
  --ks-pass env:APK_RELEASE_PASSWORD --key-pass env:APK_RELEASE_PASSWORD \
  --v4-signing-enabled false --out "$2" "$input"
"$ANDROID_BUILD_TOOLS/apksigner" verify --verbose --print-certs "$2"
"$ANDROID_BUILD_TOOLS/zipalign" -c -P 16 4 "$2"
