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

cat >> "${HOME}/.gradle/gradle.properties" <<EOF
org.librarysimplified.drm.enabled=true

org.librarysimplified.nexus.depend=true
org.librarysimplified.nexus.username=${NYPL_NEXUS_USER}
org.librarysimplified.nexus.password=${NYPL_NEXUS_PASSWORD}

org.librarysimplified.app.assets.openebooks=${CREDENTIALS_PATH}/OpenEBooks/Android/
org.librarysimplified.app.assets.simplye=${CREDENTIALS_PATH}/credentials/SimplyE/Android/
EOF
