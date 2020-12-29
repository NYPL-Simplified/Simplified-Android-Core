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

info "installing Adobe DRM certificate"

cp -v ".ci/credentials/OpenEbooks/Android/ReaderClientCert.sig" \
  "simplified-app-openebooks/src/main/assets/ReaderClientCert.sig" || exit 1
cp -v ".ci/credentials/SimplyE/Android/ReaderClientCert.sig" \
  "simplified-app-simplye/src/main/assets/ReaderClientCert.sig" || exit 1

info "installing crashlytics configuration"

cp -v ".ci/credentials/OpenEbooks/Android/google-services.json" \
   "simplified-app-openebooks/google-services.json" || exit 1
cp -v ".ci/credentials/SimplyE/Android/google-services.json" \
   "simplified-app-simplye/google-services.json" || exit 1

info "installing cardcreator configuration"

cp -v ".ci/credentials/SimplyE/Android/cardcreator.conf" \
  "simplified-app-simplye/src/main/assets/cardcreator.conf" || exit 1

info "installing feedbooks.conf"

cp -v ".ci/credentials/Feedbooks/feedbooks.conf" \
  "simplified-app-simplye/src/main/assets/feedbooks.conf" || exit 1

info "installing overdrive.conf"

cp -v ".ci/credentials/Overdrive/audiobook_fulfillment.json" \
  "simplified-app-simplye/src/main/assets/overdrive.json" || exit 1

#------------------------------------------------------------------------
# Add the NYPL nexus properties to the project properties.
#

mkdir -p "${HOME}/.gradle" ||
  fatal "could not create ${HOME}/.gradle"

cat >> "${HOME}/.gradle/gradle.properties" << EOF
org.librarysimplified.nexus.username=${NYPL_NEXUS_USER}
org.librarysimplified.nexus.password=${NYPL_NEXUS_PASSWORD}
EOF

cat ".ci/credentials/APK Signing/nypl-keystore.properties" >> "${HOME}/.gradle/gradle.properties" ||
  fatal "could not read keystore properties"
