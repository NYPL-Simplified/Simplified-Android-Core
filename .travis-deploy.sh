#!/bin/sh

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

#------------------------------------------------------------------------
# Clone binaries repos

info "cloning binaries"

git clone \
  --depth 1 \
  --single-branch \
  --branch develop \
  "https://${NYPL_GITHUB_ACCESS_TOKEN}@github.com/NYPL-Simplified/android-binaries" \
  ".travis/binaries" \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not clone binaries"

./.travis-git-props.sh > ".travis/build.properties" ||
  fatal "could not save build properties"
./.travis-git-message.sh > ".travis/commit-message.txt" ||
  fatal "could not save commit message"


#------------------------------------------------------------------------
# Archive the build artifacts

WORKING_DIRECTORY=$(pwd) ||
  fatal "could not save working directory"

BINARIES_DIRECTORY="${WORKING_DIRECTORY}/.travis/binaries"

cd "${BINARIES_DIRECTORY}" ||
  fatal "could not switch to binaries directory"

git rm -f *.apk
git rm -f build.properties

cd "${WORKING_DIRECTORY}" ||
  fatal "could not restore working directory"

find ./simplified-app-vanilla -name *.apk -exec cp -v {} "${BINARIES_DIRECTORY}" \;

cp -v "${WORKING_DIRECTORY}/.travis/build.properties" "${BINARIES_DIRECTORY}"/build.properties ||
  fatal "could not copy build properties"

cd "${BINARIES_DIRECTORY}" ||
  fatal "could not switch to binaries directory"

git add *.apk ||
  fatal "could not add APKs to index"
git add build.properties ||
  fatal "could not add build properties to index"
git commit --file="${WORKING_DIRECTORY}/.travis/commit-message.txt" ||
  fatal "could not commit"

git push --force || fatal "could not push"
