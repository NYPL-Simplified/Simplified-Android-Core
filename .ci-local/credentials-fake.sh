#!/bin/bash

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "credentials-fake.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "credentials-fake.sh: info: $1" 1>&2
}

mkdir -p "${HOME}/.gradle" ||
  fatal "could not create ${HOME}/.gradle"

#------------------------------------------------------------------------
# Generate a temporary keystore for APK signing.
#
# This is a keystore used to sign the APK during PR builds, but these APK
# files are immediately discarded and therefore the signing key is irrelevant.
#

info "generating keystore"

keytool \
  -genkey \
  -v \
  -keystore release.jks \
  -alias FAKE \
  -keyalg RSA \
  -keysize 2048 \
  -keypass redherring \
  -storepass redherring \
  -dname 'CN=org.librarysimplified,OU=android,O=NYPL,L=NYC,S=NY,C=US' \
  -validity 2

cat >> "${HOME}/.gradle/gradle.properties" <<EOF
org.librarysimplified.keyAlias=FAKE
org.librarysimplified.keyPassword=redherring
org.librarysimplified.storePassword=redherring
EOF
