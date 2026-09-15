#!/bin/sh
set -eu
LISTEN_PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
LISTEN_TEST_OUT=$(mktemp -d "${TMPDIR:-/tmp}/airpods-listening-tests.XXXXXX")
javac -encoding UTF-8 -source 8 -target 8 -d "$LISTEN_TEST_OUT" \
 "$LISTEN_PROJECT/src/com/chocho/airpodsglance/BatteryComponent.java" \
 "$LISTEN_PROJECT/src/com/chocho/airpodsglance/AapBatteryDecoder.java" \
 "$LISTEN_PROJECT/src/com/chocho/airpodsglance/ListeningProtocol.java" \
 "$LISTEN_PROJECT/src/com/chocho/airpodsglance/ListeningState.java" \
 "$LISTEN_PROJECT/src/com/chocho/airpodsglance/AapConnectionOwner.java" \
 "$LISTEN_PROJECT/tests/com/chocho/airpodsglance/ListeningTests.java"
java -cp "$LISTEN_TEST_OUT" com.chocho.airpodsglance.ListeningTests
