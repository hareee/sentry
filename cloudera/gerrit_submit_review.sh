#!/usr/bin/env bash
#
# This script 'gerrit_submit_review.sh' helps Sentry developers to submit
# their current commits to the Gerrit review board.
#

 set -e

# Change directory to hive root directory
cd `dirname $0`/..

BRANCH="$1"
REVIEWERS="$2"

SCRIPT_NAME=`basename $0`
function show_help() {
  echo "Usage: $SCRIPT_NAME <branch> [r=<reviewer>[,r=<reviewer>[,...]]]"
  echo "i.e. $SCRIPT_NAME cdh6.x"
  echo
}

if [ -z "$BRANCH" ]; then
  show_help && exit 1
fi

if [ -n "$REVIEWERS" ]; then
  git push cauldron HEAD:refs/for/$BRANCH%$REVIEWERS
else
  git push cauldron HEAD:refs/for/$BRANCH
fi
