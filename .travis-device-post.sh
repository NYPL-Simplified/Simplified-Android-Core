#!/bin/sh

#------------------------------------------------------------------------
# Do all of the nonsense necessary to get on-device tests to be somewhat
# reliable
#------------------------------------------------------------------------

android-wait-for-emulator

adb shell input keyevent 82 &
adb shell svc power stayon true &
adb shell settings put global window_animation_scale 0 &
adb shell settings put global transition_animation_scale 0 &
adb shell settings put global animator_duration_scale 0 &

./gradlew connectedAndroidTest

adb logcat -d | awk NF
