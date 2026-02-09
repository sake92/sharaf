#!/bin/bash
# Copy example files to the docs/_includes folder

set -e

examples=(
    "examples/scala-cli/hello.sc"
    "examples/scala-cli/path_params.sc"
    "examples/scala-cli/query_params.sc"
    "examples/scala-cli/static_files.sc"
    "examples/scala-cli/json_api.test.scala"
    "examples/scala-cli/validation.sc"
    "examples/scala-cli/html.sc"
    "examples/scala-cli/form_handling.sc"
    "examples/htmx/htmx_load_snippet.sc"
)

target_folder="docs/_includes"

echo "Copying example files to ${target_folder}..."

for file in "${examples[@]}"; do
    if [ -f "$file" ]; then
        cp -f "$file" "$target_folder/"
        echo "  Copied: $file"
    else
        echo "  Warning: $file not found"
    fi
done

echo "Done copying examples."
