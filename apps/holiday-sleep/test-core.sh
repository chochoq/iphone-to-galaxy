#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TEST_DIR=$(mktemp -d "${TMPDIR:-/tmp}/holiday-sleep-core.XXXXXX")
trap 'rm -r "$TEST_DIR"' 0
javac -encoding UTF-8 -d "$TEST_DIR" \
  "$PROJECT_DIR/test/stubs/android/content/Context.java" \
  "$PROJECT_DIR/test/stubs/android/content/SharedPreferences.java" \
  "$PROJECT_DIR/test/stubs/android/content/Intent.java" \
  "$PROJECT_DIR/test/stubs/android/app/PendingIntent.java" \
  "$PROJECT_DIR/test/stubs/android/app/AlarmManager.java" \
  "$PROJECT_DIR/test/stubs/android/os/Build.java" \
  "$PROJECT_DIR/test/stubs/com/chocho/holidaysleep/HolidayCalendar.java" \
  "$PROJECT_DIR/test/stubs/com/chocho/holidaysleep/HolidayAlarmReceiver.java" \
  "$PROJECT_DIR/test/stubs/com/chocho/holidaysleep/ZenController.java" \
  "$PROJECT_DIR/src/com/chocho/holidaysleep/ScheduleWindow.java" \
  "$PROJECT_DIR/src/com/chocho/holidaysleep/ScheduleSettings.java" \
  "$PROJECT_DIR/src/com/chocho/holidaysleep/Scheduler.java" \
  "$PROJECT_DIR/test/com/chocho/holidaysleep/ScheduleWindowTest.java" \
  "$PROJECT_DIR/test/com/chocho/holidaysleep/ScheduleSettingsTest.java" \
  "$PROJECT_DIR/test/com/chocho/holidaysleep/SchedulerTest.java"
java -cp "$TEST_DIR" com.chocho.holidaysleep.ScheduleWindowTest
java -cp "$TEST_DIR" com.chocho.holidaysleep.ScheduleSettingsTest
java -cp "$TEST_DIR" com.chocho.holidaysleep.SchedulerTest
