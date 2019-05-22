#!/bin/sh -x

fatal()
{
  echo "fatal: $1" 1>&2
  cat .travis/device-pre.txt
  exit 1
}

mkdir -p .travis || fatal "could not create .travis"

#------------------------------------------------------------------------
# Download avdmanager

yes | sdkmanager tools \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not download avdmanager"

#------------------------------------------------------------------------
# Create the emulator and start it

echo no | avdmanager create avd \
  --name test \
  --force \
  --package 'system-images;android-21;default;armeabi-v7a' \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not create AVD"

emulator -avd test -no-audio -no-window & \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not start AVD"

#------------------------------------------------------------------------
# Install SDKs

yes | sdkmanager "platforms;android-28" \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not install platform"

yes | sdkmanager --update \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not update platform"

yes | sdkmanager --licenses \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not agree to licenses"

