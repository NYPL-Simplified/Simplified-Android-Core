#!/bin/sh

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

(cat <<EOF
org.librarysimplified.keyAlias=nypl
org.librarysimplified.keyPassword=${NYPL_SIGNING_KEY_PASSWORD}
org.librarysimplified.storePassword=${NYPL_SIGNING_STORE_PASSWORD}
org.gradle.parallel=false

EOF
) > gradle.properties.extra || exit 1

cat gradle.properties gradle.properties.extra > gradle.properties.tmp || exit 1

mv gradle.properties.tmp gradle.properties || exit 1

cat gradle.properties

exec ./gradlew clean assemble test
