#!/usr/bin/env bash
set -euo pipefail

# Keep this selection aligned with the Publish steps in the release and CI workflows.
poms=$(deder exec -t pomSettings -m '~%-test%' -m '~examples%' --format=json)
coordinates=$(jq -er '
  if (.failures | length) > 0 then error("Failed to resolve release coordinates") else . end
  | [.results[] | select(. != null) | "\(.groupId):\(.artifactId):\(.version)"]
  | unique
  | if length == 0 then error("No release coordinates found") else join("\n") end
' <<< "$poms")

{
  echo '## Dependency update prompt'
  echo
  echo '```text'
  echo "Update this project's existing Sharaf, Formson, Querson, and Validson dependencies to the versions below."
  echo 'Use the Maven coordinates matching its Scala version and platform; only update dependencies it already uses.'
  echo 'Update the build configuration and run the relevant compile and test checks.'
  echo
  echo 'Published Maven coordinates (groupId:artifactId:version):'
  echo "$coordinates"
  if jq -e 'any(.results[]; . != null and (.version | endswith("-SNAPSHOT")))' <<< "$poms" > /dev/null; then
    echo
    echo 'For these snapshot versions, add the snapshot repository if needed:'
    echo 'https://central.sonatype.com/repository/maven-snapshots/'
  fi
  echo '```'
} | tee "${GITHUB_STEP_SUMMARY:-/dev/null}"
