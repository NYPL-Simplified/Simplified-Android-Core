#!/bin/sh

fatal()
{
  echo "fatal: $1" 1>&2
  echo
  find "${ANDROID_HOME}/system-images"
  echo
  echo "dumping log: " 1>&2
  echo
  echo
  cat .travis/device-pre.txt
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

mkdir -p .travis || fatal "could not create .travis"

info "dumping environment"

export ANDROID_SDK_ROOT="${ANDROID_HOME}"

env | sort -u

#------------------------------------------------------------------------
# Download avdmanager

info "downloading avdmanager"

yes | sdkmanager tools \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not download avdmanager"

info "avdmanager: $(which avdmanager)"

info "downloading emulator"

yes | sdkmanager emulator \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not download emulator"

info "installing platforms"

yes | sdkmanager "platforms;android-28" \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not install platform"

info "installing system image"

yes | sdkmanager "system-images;android-21;default;armeabi-v7a" \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not install system image"

info "updating platforms"

yes | sdkmanager --update \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not update platform"

info "agreeing to licenses"

yes | sdkmanager --licenses \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not agree to licenses"

#------------------------------------------------------------------------
# Create the emulator and start it

info "creating an AVD"

echo no | avdmanager create avd \
  --name test \
  --force \
  --package 'system-images;android-21;default;armeabi-v7a' \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not create AVD"

info "starting an emulator"

$ANDROID_HOME/emulator/emulator -avd test -no-audio -no-window & \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not start AVD"

EMULATOR_PID=$!

info "waiting a few seconds for emulator startup"

EMULATOR_WAITED=0
EMULATOR_WAIT_MAX=20

while [ 1 ]
do
  sleep 2

  kill -0 "${EMULATOR_PID}"
  if [ $? -ne 0 ]
  then
    fatal "emulator failed to run"
  else
    if [ ${EMULATOR_WAITED} -gt ${EMULATOR_WAIT_MAX} ]
    then
      info "finished waiting for emulator"
      break
    else
      info "waiting for emulator"
      EMULATOR_WAITED=$(expr ${EMULATOR_WAITED} + 2)
    fi
  fi
done

