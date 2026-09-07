#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$PROJECT_DIR/../../tools/android-env.sh"
android_resolve_tools 36.0.0 36
AAPT="$ANDROID_BUILD_TOOLS/aapt"
APK="$PROJECT_DIR/build/android/airpods-glance.apk"

permissions=$("$AAPT" dump permissions "$APK")
for forbidden in \
  android.permission.INTERNET \
  android.permission.ACCESS_FINE_LOCATION \
  android.permission.ACCESS_COARSE_LOCATION \
  android.permission.BIND_ACCESSIBILITY_SERVICE \
  android.permission.RECORD_AUDIO
do
  if printf '%s\n' "$permissions" | grep -F "$forbidden" >/dev/null; then
    printf 'Forbidden permission found: %s\n' "$forbidden" >&2
    exit 1
  fi
done

printf '%s\n' "$permissions" | grep -F "android.permission.BLUETOOTH_SCAN" >/dev/null
printf '%s\n' "$permissions" | grep -F "android.permission.BLUETOOTH_CONNECT" >/dev/null
printf '%s\n' "$permissions" | grep -F "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" >/dev/null
printf '%s\n' "$permissions" | grep -F "android.permission.SYSTEM_ALERT_WINDOW" >/dev/null

xml=$("$AAPT" dump xmltree "$APK" AndroidManifest.xml)
printf '%s\n' "$xml" | grep -F '.AirPodsWidgetProvider' >/dev/null
printf '%s\n' "$xml" | grep -F 'foregroundServiceType' >/dev/null

"$ANDROID_BUILD_TOOLS/apksigner" verify "$APK"
printf 'APK policy checks passed\n'
