#!/bin/bash
set -eo pipefail

FOLDER="$1"
if [[ -z "$FOLDER" ]]; then
    echo "Error: Must specify folder!"
    exit 1
fi

cd "$FOLDER"
shopt -s nullglob
for example in *.sc *.scala; do
  case "$example" in
    project.scala|*.test.scala) continue ;;
  esac
  echo "Compiling $example"
  scala-cli compile "$example" --server=false
done
