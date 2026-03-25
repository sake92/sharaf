#!/bin/bash
set -eo pipefail

FOLDER="$1"
if [[ -z "$FOLDER" ]]; then
    echo "Error: Must specify folder!"
    exit 1
fi

cd $FOLDER
for example in *.sc; do
  echo "Compiling $example"
  scala compile "$example"
done
