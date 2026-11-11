#!/bin/bash
# Screenshots of the -Pdemo -Pshots build in one language, opened straight on each screen by intent extras.
# Usage: tools/capture.sh en|ru   (ANDROID_SERIAL picks the phone; the 13 Pro by default)
set -e
export ANDROID_SERIAL=${ANDROID_SERIAL:-a78f414d} JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "$(dirname "$0")/.."
L=$1; P=app.puffling.demo; OUT=docs/screenshots/$L; mkdir -p $OUT
./gradlew assembleDebug -Pdemo -Pshots -Pdemolang=$L -q 2>&1 | grep -E "^e: |FAILED" && exit 1
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell pm clear $P >/dev/null
adb shell am start -n $P/app.puffling.MainActivity >/dev/null; sleep 75
shot() { [ "$(adb shell dumpsys telephony.registry | grep -m1 mCallState | tr -dc 0-9)" = "0" ] || { echo "call"; exit 1; }
  adb shell am start -S -n $P/app.puffling.MainActivity $2 >/dev/null; sleep ${3:-6}
  f=$(adb shell dumpsys window | grep -m1 mCurrentFocus); echo "$f" | grep -q "$P" || { echo "focus lost: $f"; exit 1; }
  adb exec-out screencap -p > $OUT/$1.png; }
shot 1-home "--ei tab 0" 8
shot 2-practices "--ei tab 1"
shot 3-wimhof "--ei tab 1 --es open begin:wimhof"
shot 4-diary "--ei tab 3"
shot 5-summary "--ei tab 3 --es open summary"
shot 6-pets "--ei tab 2"
adb shell am force-stop $P
echo done $L
