#!/bin/sh

# Shared Android SDK discovery and local debug signing for the three small apps.
# This file is sourced by each app's build script.

android_resolve_tools() {
  build_tools_version=${1:-36.0.0}
  platform_version=${2:-36}

  android_sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
  if [ -z "$android_sdk_root" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
      android_sdk_root="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
      android_sdk_root="$HOME/Android/Sdk"
    else
      printf '%s\n' 'Set ANDROID_SDK_ROOT to your Android SDK directory.' >&2
      return 1
    fi
  fi

  ANDROID_BUILD_TOOLS="$android_sdk_root/build-tools/$build_tools_version"
  ANDROID_JAR="$android_sdk_root/platforms/android-$platform_version/android.jar"
  for required_tool in aapt aapt2 apksigner d8 zipalign; do
    if [ ! -x "$ANDROID_BUILD_TOOLS/$required_tool" ]; then
      printf 'Missing Android build tool: %s\n' \
        "$ANDROID_BUILD_TOOLS/$required_tool" >&2
      return 1
    fi
  done
  if [ ! -f "$ANDROID_JAR" ]; then
    printf 'Missing Android platform: %s\n' "$ANDROID_JAR" >&2
    return 1
  fi
  export ANDROID_BUILD_TOOLS ANDROID_JAR
}

android_sign_debug() {
  aligned_apk=$1
  output_apk=$2
  key_alias=$3
  local_dir=$4
  debug_keystore="$local_dir/debug.jks"
  debug_password=${ANDROID_DEBUG_KEYSTORE_PASSWORD:-android}

  mkdir -p "$local_dir"
  if [ ! -f "$debug_keystore" ]; then
    keytool -genkeypair \
      -keystore "$debug_keystore" \
      -storepass "$debug_password" \
      -keypass "$debug_password" \
      -alias "$key_alias" \
      -dname "CN=Android Debug, O=iPhone to Galaxy, C=KR" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -noprompt
  fi

  "$ANDROID_BUILD_TOOLS/apksigner" sign \
    --ks "$debug_keystore" \
    --ks-key-alias "$key_alias" \
    --ks-pass "pass:$debug_password" \
    --key-pass "pass:$debug_password" \
    --out "$output_apk" \
    "$aligned_apk"
  "$ANDROID_BUILD_TOOLS/apksigner" verify --verbose "$output_apk"
}
