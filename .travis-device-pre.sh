#!/bin/sh -x

fatal()
{
  echo "fatal: $1" 1>&2
  cat .travis/device-pre.txt
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

mkdir -p .travis || fatal "could not create .travis"

#------------------------------------------------------------------------
# Download avdmanager

info "downloading avdmanager"

yes | sdkmanager tools \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not download avdmanager"

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

emulator -avd test -no-audio -no-window & \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not start AVD"

#------------------------------------------------------------------------
# Install SDKs

info "installing platforms"

yes | sdkmanager "platforms;android-28" \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not install platform"

info "updating platforms"

yes | sdkmanager --update \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not update platform"

info "agreeing to licenses"

yes | sdkmanager --licenses \
  >> .travis/device-pre.txt 2>&1 \
  || fatal "could not agree to licenses"

