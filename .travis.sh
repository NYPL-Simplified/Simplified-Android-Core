#!/bin/sh

if [ -z "${NYPL_NEXUS_USER}" ]
then
  echo "error: NYPL_NEXUS_USER is not defined" 1>&2
  exit 1
fi

if [ -z "${NYPL_NEXUS_PASSWORD}" ]
then
  echo "error: NYPL_NEXUS_PASSWORD is not defined" 1>&2
  exit 1
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

#------------------------------------------------------------------------
# Clone GitHub repos

mkdir -p .travis || exit 1

(cat gradle.properties <<EOF
org.librarysimplified.nexus.username=${NYPL_NEXUS_USER}
org.librarysimplified.nexus.password=${NYPL_NEXUS_PASSWORD}

org.librarysimplified.with_bugsnag=true
org.librarysimplified.with_drm_adobe=true
org.librarysimplified.with_findaway=true

org.librarysimplified.simplye.keyAlias=nypl
org.librarysimplified.simplye.keyPassword=${NYPL_SIGNING_KEY_PASSWORD}
org.librarysimplified.simplye.storePassword=${NYPL_SIGNING_STORE_PASSWORD}

org.gradle.parallel=false
EOF
) > gradle.properties.tmp || exit 1

mv gradle.properties.tmp gradle.properties || exit 1

cat gradle.properties

exec ./gradlew clean assemble test
