#!/bin/sh
set -eu
WIDGET_PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WIDGET_TEST_OUT=$(mktemp -d "${TMPDIR:-/tmp}/airpods-listening-widget-tests.XXXXXX")
javac -encoding UTF-8 -source 8 -target 8 -d "$WIDGET_TEST_OUT" \
 "$WIDGET_PROJECT/src/com/chocho/airpodsglance/ListeningProtocol.java" \
 "$WIDGET_PROJECT/src/com/chocho/airpodsglance/ListeningState.java" \
 "$WIDGET_PROJECT/src/com/chocho/airpodsglance/ListeningWidgetOptions.java" \
 "$WIDGET_PROJECT/src/com/chocho/airpodsglance/ListeningOrderDraft.java" \
 "$WIDGET_PROJECT/src/com/chocho/airpodsglance/ListeningWidgetRequest.java" \
 "$WIDGET_PROJECT/tests/com/chocho/airpodsglance/ListeningWidgetTests.java" \
 "$WIDGET_PROJECT/tests/com/chocho/airpodsglance/ListeningOrderDraftTests.java"
java -cp "$WIDGET_TEST_OUT" com.chocho.airpodsglance.ListeningWidgetTests
java -cp "$WIDGET_TEST_OUT" com.chocho.airpodsglance.ListeningOrderDraftTests
