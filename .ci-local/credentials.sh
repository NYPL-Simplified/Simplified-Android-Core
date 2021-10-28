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

if [ -z "${NYPL_S3_ACCESS_KEY}" ]
then
  fatal "NYPL_S3_ACCESS_KEY is not defined"
fi
if [ -z "${NYPL_S3_SECRET_ACCESS_KEY}" ]
then
  fatal "NYPL_S3_SECRET_ACCESS_KEY is not defined"
fi

#------------------------------------------------------------------------
# Copy credentials into place.
#

info "installing keystore"

cp -v ".ci/credentials/APK Signing/nypl-keystore.jks" \
  "release.jks" || exit 1

#------------------------------------------------------------------------
# Add the NYPL nexus properties to the project properties.
#

mkdir -p "${HOME}/.gradle" ||
  fatal "could not create ${HOME}/.gradle"

cat ".ci/credentials/APK Signing/nypl-keystore.properties" >> "${HOME}/.gradle/gradle.properties" ||
  fatal "could not read keystore properties"

CREDENTIALS_PATH=$(realpath ".ci/credentials") ||
  fatal "could not resolve credentials path"

SIMPLYE_CREDENTIALS="${CREDENTIALS_PATH}/SimplyE/Android"
OPENEBOOKS_CREDENTIALS="${CREDENTIALS_PATH}/OpenEbooks/Android"

if [ ! -d "${SIMPLYE_CREDENTIALS}" ]
then
  fatal "${SIMPLYE_CREDENTIALS} does not exist, or is not a directory"
fi
if [ ! -d "${OPENEBOOKS_CREDENTIALS}" ]
then
  fatal "${OPENEBOOKS_CREDENTIALS} does not exist, or is not a directory"
fi

cp "${CREDENTIALS_PATH}/PlayStore/play_store_api_key.json" "simplified-app-simplye/play_store_api_key.json" ||
  fatal "could not copy Play Store key"
cp "${CREDENTIALS_PATH}/PlayStore/play_store_api_key.json" "simplified-app-openebooks/play_store_api_key.json" ||
  fatal "could not copy Play Store key"

cat >> "${HOME}/.gradle/gradle.properties" <<EOF
org.librarysimplified.drm.enabled=true

org.librarysimplified.s3.depend=true
org.librarysimplified.s3.access_key_id=${NYPL_S3_ACCESS_KEY}
org.librarysimplified.s3.secret_access_key=${NYPL_S3_SECRET_ACCESS_KEY}

org.librarysimplified.app.assets.openebooks=${OPENEBOOKS_CREDENTIALS}
org.librarysimplified.app.assets.simplye=${SIMPLYE_CREDENTIALS}
EOF

#------------------------------------------------------------------------
# Addding slack webhook to environment
SLACK_WEBHOOK_URL=$(<.ci/credentials/SimplyE/slack-webhook.url) || 
  fatal "Slack Webhook url not found."
cat >> ".env" <<EOF
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL}"
EOF
