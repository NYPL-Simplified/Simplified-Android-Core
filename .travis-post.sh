#!/bin/sh

fatal()
{
  echo "fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

WORKING_DIRECTORY=$(pwd) ||
  fatal "could not save working directory"

BINARIES_DIRECTORY="${WORKING_DIRECTORY}/.travis/binaries"

cd "${BINARIES_DIRECTORY}" ||
  fatal "could not switch to binaries directory"

git rm -f *.apk
git rm -f build.properties

cd "${WORKING_DIRECTORY}" ||
  fatal "could not restore working directory"

cp -v ./simplified-app-simplye/build/outputs/apk/debug/*.apk   "${BINARIES_DIRECTORY}"
cp -v ./simplified-app-simplye/build/outputs/apk/release/*.apk "${BINARIES_DIRECTORY}"
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
