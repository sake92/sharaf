#!/bin/bash
set -eo pipefail

VERSION="$1"
if [[ -z "$VERSION" ]]; then
    echo "Error: Must specify version!"
    exit 1
fi

if git rev-parse -q --verify "refs/tags/$VERSION" >/dev/null; then
    echo "Tag '$VERSION' already exists!"
    exit 1
fi

git commit --allow-empty -am "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push --atomic origin main $VERSION

