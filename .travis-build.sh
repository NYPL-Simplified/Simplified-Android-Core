#!/bin/sh

#------------------------------------------------------------------------
# Configure some Gradle settings that work better in CI containers

cat <<EOF

org.gradle.daemon=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError

EOF >> gradle.properties

#------------------------------------------------------------------------
# Execute the build

./gradlew clean test assemble \
  -Porg.librarysimplified.keyAlias=nypl \
  -Porg.librarysimplified.keyPassword=${NYPL_SIGNING_KEY_PASSWORD} \
  -Porg.librarysimplified.storePassword=${NYPL_SIGNING_STORE_PASSWORD}

