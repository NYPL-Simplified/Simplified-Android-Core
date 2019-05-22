#!/bin/sh -x

echo "info: starting up emulator" 1>&2

#------------------------------------------------------------------------
# Download avdmanager

yes | sdkmanager tools || exit 1

#------------------------------------------------------------------------
# Create the emulator and start it

avdmanager list target || exit 1
avdmanager create avd --name test --force --package 'system-images;android-21;default;armeabi-v7a' || exit 1
emulator -avd test -no-audio -no-window &

#------------------------------------------------------------------------
# Install SDKs

echo "info: installing Android SDKs" 1>&2

yes | sdkmanager "platforms;android-28"
yes | sdkmanager --update
yes | sdkmanager --licenses
