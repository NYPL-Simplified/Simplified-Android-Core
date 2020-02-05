#!/bin/sh

#------------------------------------------------------------------------
# Execute the build

./gradlew clean test assemble \
  -Porg.librarysimplified.keyAlias=nypl \
  -Porg.librarysimplified.keyPassword=${NYPL_SIGNING_KEY_PASSWORD} \
  -Porg.librarysimplified.storePassword=${NYPL_SIGNING_STORE_PASSWORD} \
  -Porg.gradle.configureondemand=true \
  -Porg.gradle.jvmargs="-Xmx4g -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError"

