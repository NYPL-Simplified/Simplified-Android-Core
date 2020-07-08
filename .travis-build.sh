#!/bin/sh

#------------------------------------------------------------------------
# Execute the build

./gradlew clean ktlint test assemble \
  -Dorg.gradle.internal.publish.checksums.insecure=true                 \
  -Porg.librarysimplified.keyAlias=nypl                                 \
  -Porg.librarysimplified.keyPassword=${NYPL_SIGNING_KEY_PASSWORD}      \
  -Porg.librarysimplified.storePassword=${NYPL_SIGNING_STORE_PASSWORD}  \
  -Porg.gradle.parallel=false

