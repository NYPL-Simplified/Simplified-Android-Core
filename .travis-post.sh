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

cp -v ./simplified-app-simplye/build/outputs/apk/debug/*   "${BINARIES_DIRECTORY}"
cp -v ./simplified-app-simplye/build/outputs/apk/release/* "${BINARIES_DIRECTORY}"

./.travis-git-props.sh > "${BINARIES_DIRECTORY}/build.properties" ||
  fatal "could not save build properties"
./.travis-git-message.sh > "${WORKING_DIRECTORY}/commit-message.txt" ||
  fatal "could not save commit message"

cd "${BINARIES_DIRECTORY}" ||
  fatal "could not switch to binaries directory"

git add *.apk ||
  fatal "could not add APKs to index"
git add build.properties ||
  fatal "could not add build properties to index"
git commit --file="${WORKING_DIRECTORY}/commit-message.txt" ||
  fatal "could not commit"
git push ||
  fatal "could not push"

