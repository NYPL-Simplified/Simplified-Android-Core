#!/bin/sh

fatal()
{
  echo "fatal: $1" 1>&2
  echo
  echo "dumping log: " 1>&2
  echo
  cat .travis/pre.txt
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
  >> .travis/pre.txt 2>&1 \
  || fatal "could not download avdmanager"

info "avdmanager: $(which avdmanager)"

COMPONENTS="
build-tools;28.0.3
platform-tools
platforms;android-28
tools
"

for COMPONENT in ${COMPONENTS}
do
  info "downloading ${COMPONENT}"

  yes | sdkmanager "${COMPONENT}" \
    >> .travis/pre.txt 2>&1 \
    || fatal "could not download emulator"
done

info "updating all"

yes | sdkmanager --update \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not update platform"

info "agreeing to licenses"

yes | sdkmanager --licenses \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not agree to licenses"

