#!/bin/sh

#------------------------------------------------------------------------
# Execute the build

exec ./gradlew clean test assemble \
  -Porg.librarysimplified.keyAlias=nypl \
  -Porg.librarysimplified.keyPassword=${NYPL_SIGNING_KEY_PASSWORD} \
  -Porg.librarysimplified.storePassword=${NYPL_SIGNING_STORE_PASSWORD} \
  -Porg.gradle.configureondemand=true \
  -Porg.gradle.jvmargs="-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError" \
  -Porg.gradle.parallel=false

