#!/bin/bash

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "credentials-local.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "credentials-local.sh: info: $1" 1>&2
}

if [ -z "${NYPL_NEXUS_USER}" ]
then
  fatal "NYPL_NEXUS_USER is not defined"
fi
if [ -z "${NYPL_NEXUS_PASSWORD}" ]
then
  fatal "NYPL_NEXUS_PASSWORD is not defined"
fi

#------------------------------------------------------------------------
# Copy credentials into place.
#

info "installing keystore"

cp -v ".ci/credentials/APK Signing/nypl-keystore.jks" \
  "release.jks" || exit 1

#------------------------------------------------------------------------
# Add the NYPL nexus properties to the project properties.
#

mkdir -p "${HOME}/.gradle" ||
  fatal "could not create ${HOME}/.gradle"

cat ".ci/credentials/APK Signing/nypl-keystore.properties" >> "${HOME}/.gradle/gradle.properties" ||
  fatal "could not read keystore properties"

CREDENTIALS_PATH=$(realpath ".ci/credentials") ||
  fatal "could not resolve credentials path"

SIMPLYE_CREDENTIALS="${CREDENTIALS_PATH}/SimplyE/Android"
OPENEBOOKS_CREDENTIALS="${CREDENTIALS_PATH}/OpenEBooks/Android"

if [ ! -d "${SIMPLYE_CREDENTIALS}" ]
then
  fatal "${SIMPLYE_CREDENTIALS} does not exist, or is not a directory"
fi
if [ ! -d "${OPENEBOOKS_CREDENTIALS}" ]
then
  fatal "${OPENEBOOKS_CREDENTIALS} does not exist, or is not a directory"
fi

cat >> "${HOME}/.gradle/gradle.properties" <<EOF
org.librarysimplified.drm.enabled=true

org.librarysimplified.nexus.depend=true
org.librarysimplified.nexus.username=${NYPL_NEXUS_USER}
org.librarysimplified.nexus.password=${NYPL_NEXUS_PASSWORD}

org.librarysimplified.app.assets.openebooks=${OPENEBOOKS_CREDENTIALS}
org.librarysimplified.app.assets.simplye=${SIMPLYE_CREDENTIALS}
EOF
