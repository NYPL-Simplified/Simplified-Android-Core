#!/bin/sh

GP_GIT_COMMIT=$(git rev-list --max-count=1 HEAD) || exit 1
GP_GIT_BRANCH=$(git branch | grep '^\*' | awk '{print $NF}') || exit 1
SIMPLYE_VERSION=$(grep versionCode simplified-app-simplye/version.properties | sed 's/versionCode=//g') || exit 1

cat <<EOF
Build of ${GP_GIT_COMMIT}

Git commit: ${GP_GIT_COMMIT}
Git branch: ${GP_GIT_BRANCH}
Version code: ${SIMPLYE_VERSION}

Commit link: https://www.github.com/NYPL-Simplified/android/commit/${GP_GIT_COMMIT}
EOF
