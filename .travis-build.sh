#!/bin/sh

#------------------------------------------------------------------------
# Execute the build

exec ./gradlew clean test assemble \
  -Porg.librarysimplified.keyAlias=nypl \
  -Porg.librarysimplified.keyPassword=${NYPL_SIGNING_KEY_PASSWORD} \
  -Porg.librarysimplified.storePassword=${NYPL_SIGNING_STORE_PASSWORD} \
  -Porg.gradle.parallel=false

