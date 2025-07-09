
# a one off script to copy some examples to the docs folder
$examplesList = @(
    "examples/scala-cli/hello.sc",
    "examples/scala-cli/path_params.sc",
    "examples/scala-cli/query_params.sc",
    "examples/scala-cli/static_files.sc",
    "examples/scala-cli/json_api.test.scala",
    "examples/scala-cli/validation.sc"
)

$targetFolder = "docs/_includes"

foreach ($itemToCopy in $examplesList)
{
    Copy-Item -Path $itemToCopy -Destination $targetFolder -Force
}
