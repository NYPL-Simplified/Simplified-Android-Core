#!/bin/sh

if [ -z "${NYPL_NEXUS_USER}" ]
then
  echo "error: NYPL_NEXUS_USER is not defined" 1>&2
  exit 1
fi

if [ -z "${NYPL_NEXUS_PASSWORD}" ]
then
  echo "error: NYPL_NEXUS_PASSWORD is not defined" 1>&2
  exit 1
fi

(cat <<EOF
org.librarysimplified.nexus.username=${NYPL_NEXUS_USER}
org.librarysimplified.nexus.password=${NYPL_NEXUS_PASSWORD}

org.gradle.parallel=true
org.gradle.jvmargs=-Xms1024m -Xmx2048m
org.nypl.simplified.with_findaway=true
EOF
) > gradle.properties.tmp || exit 1

mv gradle.properties.tmp gradle.properties || exit 1

./gradlew clean assembleDebug test || exit 1

android-wait-for-emulator

#------------------------------------------------------------------------
# Do all of the nonsense necessary to get on-device tests to be somewhat
# reliable
#------------------------------------------------------------------------

adb shell input keyevent 82 &
adb shell svc power stayon true &
adb shell settings put global window_animation_scale 0 &
adb shell settings put global transition_animation_scale 0 &
adb shell settings put global animator_duration_scale 0 &

./gradlew connectedAndroidTest

adb logcat -d | awk NF
