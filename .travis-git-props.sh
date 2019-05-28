#!/bin/sh

GP_GIT_COMMIT=$(git rev-list --max-count=1 HEAD) || exit 1
GP_GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD) || exit 1

cat <<EOF
git.commit: ${GP_GIT_COMMIT}
git.branch: ${GP_GIT_BRANCH}
EOF
