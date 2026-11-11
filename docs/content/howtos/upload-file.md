---
layout: howto.html
title: Upload File
description: Sharaf How To Upload File
---

# {{ page.title }}

Submit the form as `multipart/form-data` and parse the upload as a NIO `Path`.
The server adapter must support multipart forms; see the [parsing reference](/reference/parsing.html#multipart-forms).

```scala
// 1. somewhere in a view, use enctype="multipart/form-data"
html"""
    <form action="/form-submit" method="POST" enctype="multipart/form-data">
    ...
    </form>
"""

// 2. define form data class with a NIO Path file
import java.nio.file.{Files, Path as FilePath}
import ba.sake.formson.*

case class MyData(file: FilePath) derives FormDataRW

// 3. handle the file however you want
case POST -> Path("form-submit") =>
  val formData = Request.current.bodyForm[MyData]
  val fileAsString = Files.readString(formData.file)
```

You can find a working example in the [repo]({{site.data.project.gh.sourcesUrl}}/examples/fullstack).

Read or copy the upload while handling the request. The temporary file is owned
by the adapter and deleted automatically, including on parsing and handler
failures. Potateh deletes uploads before deferred response writing, so copy an
upload first if the response or background work needs it later.

To keep an upload, copy it into application-owned storage before returning:

```scala
val formData = Request.current.bodyForm[MyData]
val destination = Files.createTempFile(applicationUploadDirectory, "upload-", ".bin")
Files.copy(formData.file, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
```

Choose `applicationUploadDirectory` in your application configuration. Do not use
a client-supplied filename as a storage path. The shared parser always creates
files on disk, including empty uploads; its limits and Undertow's separate
upload behavior are listed in the [parsing reference](/reference/parsing.html#multipart-forms).
