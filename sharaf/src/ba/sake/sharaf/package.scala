package ba.sake.sharaf

import io.undertow.util.HttpString

import ba.sake.formson._

type RequestParams = (HttpString, Path)

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]

// requests integration
extension (formDataMap: FormDataMap)
  def toRequestsMultipart() = {
    val multiItems = formDataMap.flatMap { case (key, values) =>
      values.map {
        case FormValue.Str(value)       => requests.MultiItem(key, value)
        case FormValue.File(value)      => requests.MultiItem(key, value, value.getFileName.toString)
        case FormValue.ByteArray(value) => requests.MultiItem(key, value)
      }
    }
    requests.MultiPart(multiItems.toSeq*)
  }
