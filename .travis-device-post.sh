#!/bin/sh

info()
{
  echo "info: $1" 1>&2
}

dumpLogAndExit()
{
  adb logcat -d | awk NF
  exit $1
}

mkdir -p .travis || fatal "could not create .travis"

#------------------------------------------------------------------------
# Do all of the nonsense necessary to get on-device tests to be somewhat
# reliable
#------------------------------------------------------------------------

info "waiting for emulator"
while [ 1 ]
do
  BOOT_ANIM=$(adb -e shell getprop init.svc.bootanim 2>&1)
  echo "${BOOT_ANIM}" | grep "stopped"
  if [ $? -eq 0 ]
  then
    info "device booted"
    break
  else
    info "waiting for device (currently: ${BOOT_ANIM})"
    sleep 5
  fi
done

info "configuring emulator for unit tests"
adb shell input keyevent 82 &
adb shell svc power stayon true &
adb shell settings put global window_animation_scale 0 &
adb shell settings put global transition_animation_scale 0 &
adb shell settings put global animator_duration_scale 0 &

info "running device tests"
./gradlew connectedAndroidTest || dumpLogAndExit 1
