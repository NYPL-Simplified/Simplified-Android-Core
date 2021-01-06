#!/bin/sh

fatal()
{
  echo "fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "info: $1" 1>&2
  sleep 5
}

if [ $# -ne 2 ]
then
  echo "usage: username password" 1>&2
  exit 1
fi

MAVEN_CENTRAL_USERNAME="$1"
shift
MAVEN_CENTRAL_PASSWORD="$1"
shift

MAVEN_CENTRAL_STAGING_PROFILE_ID="af061f5afba777"

which wget || fatal "Please install wget"
which openssl || fatal "Please install OpenSSL"
which java || fatal "Please install java"

#------------------------------------------------------------------------
# Download Brooklime if necessary.
#

BROOKLIME_URL="https://repo1.maven.org/maven2/com/io7m/brooklime/com.io7m.brooklime.cmdline/0.0.2/com.io7m.brooklime.cmdline-0.0.2-main.jar"
BROOKLIME_SHA256_EXPECTED="abd775e9decd228e543c7ff1f9899183c57cc8b98e1b233e7d46ca03f4ee7e97"

if [ ! -f "brooklime.jar" ]
then
  wget -O "brooklime.jar" "${BROOKLIME_URL}" || fatal "could not download brooklime"
fi

BROOKLIME_SHA256_RECEIVED=$(openssl sha256 "brooklime.jar" | awk '{print $NF}') || fatal "could not checksum brooklime.jar"

if [ "${BROOKLIME_SHA256_EXPECTED}" != "${BROOKLIME_SHA256_RECEIVED}" ]
then
  fatal "brooklime.jar checksum does not match.
  Expected: ${BROOKLIME_SHA256_EXPECTED}
  Received: ${BROOKLIME_SHA256_RECEIVED}"
fi

#------------------------------------------------------------------------
# Deploy artifacts to a temporary directory.
#

TIMESTAMP=$(date "+%Y%m%d-%H%M%S") || fatal "could not create timestamp"
TEMPORARY_DIRECTORY="/tmp/simplified-${TIMESTAMP}"

info "Artifacts will temporarily be deployed to ${TEMPORARY_DIRECTORY}"
rm -rf "${TEMPORARY_DIRECTORY}" || fatal "could not ensure temporary directory is clean"
mkdir -p "${TEMPORARY_DIRECTORY}" || fatal "could not create a temporary directory"

info "Calling Gradle to publish to temporary directory"
./gradlew -Porg.librarysimplified.directory.publish="${TEMPORARY_DIRECTORY}" clean assemble publish || fatal "could not publish to directory"

#------------------------------------------------------------------------
# Create a staging repository on Maven Central.
#

info "Creating a staging repository on Maven Central"

(cat <<EOF
create
--description
Simplified ${TIMESTAMP}
--stagingProfileId
${MAVEN_CENTRAL_STAGING_PROFILE_ID}
--user
${MAVEN_CENTRAL_USERNAME}
--password
${MAVEN_CENTRAL_PASSWORD}
EOF
) > args.txt || fatal "Could not write argument file"

MAVEN_CENTRAL_STAGING_REPOSITORY_ID=$(java -jar brooklime.jar @args.txt) || fatal "Could not create staging repository"

#------------------------------------------------------------------------
# Upload content to the staging repository on Maven Central.
#

info "Uploading content to repository ${MAVEN_CENTRAL_STAGING_REPOSITORY_ID}"

(cat <<EOF
upload
--stagingProfileId
${MAVEN_CENTRAL_STAGING_PROFILE_ID}
--user
${MAVEN_CENTRAL_USERNAME}
--password
${MAVEN_CENTRAL_PASSWORD}
--directory
${TEMPORARY_DIRECTORY}
--repository
${MAVEN_CENTRAL_STAGING_REPOSITORY_ID}
EOF
) > args.txt || fatal "Could not write argument file"

java -jar brooklime.jar @args.txt || fatal "Could not upload content"

#------------------------------------------------------------------------
# Close the staging repository.
#

info "Closing repository ${MAVEN_CENTRAL_STAGING_REPOSITORY_ID}. This can take a few minutes."

(cat <<EOF
close
--stagingProfileId
${MAVEN_CENTRAL_STAGING_PROFILE_ID}
--user
${MAVEN_CENTRAL_USERNAME}
--password
${MAVEN_CENTRAL_PASSWORD}
--repository
${MAVEN_CENTRAL_STAGING_REPOSITORY_ID}
EOF
) > args.txt || fatal "Could not write argument file"

java -jar brooklime.jar @args.txt || fatal "Could not close staging repository"

#------------------------------------------------------------------------
# Release the staging repository.
#

info "Releasing repository ${MAVEN_CENTRAL_STAGING_REPOSITORY_ID}"

(cat <<EOF
release
--stagingProfileId
${MAVEN_CENTRAL_STAGING_PROFILE_ID}
--user
${MAVEN_CENTRAL_USERNAME}
--password
${MAVEN_CENTRAL_PASSWORD}
--repository
${MAVEN_CENTRAL_STAGING_REPOSITORY_ID}
EOF
) > args.txt || fatal "Could not write argument file"

java -jar brooklime.jar @args.txt || fatal "Could not release staging repository"

info "Release completed"
