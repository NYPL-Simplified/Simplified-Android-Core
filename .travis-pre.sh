#!/bin/sh

#------------------------------------------------------------------------
# Utility methods

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

#------------------------------------------------------------------------
# Check for required environment vars

if [ -z "${NYPL_GITHUB_ACCESS_TOKEN}" ]
then
  fatal "NYPL_GITHUB_ACCESS_TOKEN is not defined"
fi

if [ -z "${NYPL_SIGNING_KEY_PASSWORD}" ]
then
  echo "error: NYPL_SIGNING_KEY_PASSWORD is not defined" 1>&2
  exit 1
fi

if [ -z "${NYPL_SIGNING_STORE_PASSWORD}" ]
then
  echo "error: NYPL_SIGNING_STORE_PASSWORD is not defined" 1>&2
  exit 1
fi

mkdir -p .travis || fatal "could not create .travis"

#------------------------------------------------------------------------
# Clone credentials repos

info "cloning credentials"

git clone \
  --depth 1 \
  "https://${NYPL_GITHUB_ACCESS_TOKEN}@github.com/NYPL-Simplified/Certificates" \
  ".travis/credentials" \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not clone credentials"

#info "installing certificate"
#
#cp -v .travis/credentials/SimplyE/Android/ReaderClientCert.sig \
#  simplified-app-simplye/src/main/assets/ReaderClientCert.sig
#
#info "installing bugsnag configuration"
#
#cp -v .travis/credentials/SimplyE/Android/bugsnag.conf \
#  simplified-app-simplye/src/main/assets/bugsnag.conf

info "installing keystore"

cp -v ".travis/credentials/APK Signing/nypl-keystore.jks" \
  simplified-app-vanilla/keystore.jks
cp -v ".travis/credentials/APK Signing/nypl-keystore.jks" \
  simplified-app-vanilla-with-profiles/keystore.jks