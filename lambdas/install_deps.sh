#!/usr/bin/env bash
# Install dependencies for our Lambdas.
#
# This looks for a requirements.txt file in individual Lambdas,
# and then installs them into the Lambda directory, as required
# before the ZIP bundle is uploaded to AWS.

set -o errexit
set -o nounset

# Directory containing this script
LAMBDA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
COMMON_LIB="$LAMBDA_DIR/common"

# Install dependencies for a given Lambda directory
build_lambda() {
  lambda_dir="$1/src"
  target_dir="$1/target"

  rm -rf $target_dir
  cp -r $lambda_dir $target_dir

  echo "*** Installing $COMMON_LIB dependencies for $lambda_dir"
  cp -r "$COMMON_LIB/utils" "$target_dir"
}

for dir in $(find "$LAMBDA_DIR" \( ! -regex '.*/\..*' \) -mindepth 1 -maxdepth 1 -type d )
do
  if [ "$COMMON_LIB" != "$dir" ]; then
    build_lambda "$dir"
  fi
done