package ba.sake.sharaf.potateh

import ba.sake.potateh.{Headers, ResponseBody}
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMethodException

private[potateh] object PotatehConversions {
  private val ManagedHeaders =
    Vector("Content-Length", "Transfer-Encoding", "Connection", "Keep-Alive", "Upgrade", "Trailer")

  def parseMethod(value: String): HttpMethod = value match {
    case "GET"     => HttpMethod.GET
    case "POST"    => HttpMethod.POST
    case "PUT"     => HttpMethod.PUT
    case "DELETE"  => HttpMethod.DELETE
    case "OPTIONS" => HttpMethod.OPTIONS
    case "PATCH"   => HttpMethod.PATCH
    case "HEAD"    => HttpMethod.HEAD
    case _         => throw UnsupportedMethodException(value)
  }

  def toResponse[T](response: Response[T], requestMethod: String): ba.sake.potateh.Response = {
    // Apply explicit header updates to the body serializer's default headers.
    val contentHeaders = response.body.toSeq.flatMap(response.rw.headers)
    val resolved = response.headerUpdates.resolve(contentHeaders)
    def headerValues(name: String): Seq[String] = resolved.collect {
      case HeaderUpdate.Set(headerName, headerValues) if headerName.toString.equalsIgnoreCase(name) =>
        headerValues
    }.flatten

    // Reject connection features Potateh cannot honor on a response.
    val connectionFields = headerValues("Connection").flatMap(_.split(',')).map(_.trim)
    require(!connectionFields.exists(_.equalsIgnoreCase("close")), "Potateh does not support response-requested close")
    require(response.status.code == 426 || headerValues("Upgrade").isEmpty, "Potateh does not support protocol upgrades")
    require(headerValues("Trailer").isEmpty, "Potateh does not support response trailers")

    // Validate the requested framing before handing control to Potateh.
    val lengths = headerValues("Content-Length")
    val length = lengths.headOption.map { text =>
      require(
        lengths.size == 1 && text.nonEmpty && text.forall(c => c >= '0' && c <= '9'),
        "Invalid response Content-Length"
      )
      text.toLongOption.getOrElse(throw new IllegalArgumentException("Response Content-Length overflow"))
    }
    val transferEncoding = headerValues("Transfer-Encoding")
    require(length.isEmpty || transferEncoding.isEmpty, "Conflicting response framing")
    val knownBodyLength = response.body.flatMap(KnownBodyLength.of)
    val bodyIsSuppressed =
      requestMethod == "HEAD" || response.status.code == 204 || response.status.code == 205 ||
        response.status.code == 304
    require(
      bodyIsSuppressed || length.isEmpty || knownBodyLength.isEmpty || length == knownBodyLength,
      "Response Content-Length does not match body length"
    )

    // Leave connection and framing headers to Potateh and render cookies as headers.
    val fields = Vector.newBuilder[(name: String, value: String)]
    resolved.foreach {
      case HeaderUpdate.Set(name, headerValues)
          if (response.status.code == 426 && name.toString.equalsIgnoreCase("Upgrade")) ||
            !isManaged(name.toString, connectionFields) =>
        val headerName = name.toString
        headerValues.foreach(value => fields += ((name = headerName, value = value)))
      case _ => ()
    }
    response.cookieUpdates.updates.foreach { cookie =>
      fields += ((name = "Set-Cookie", value = CookieHeaders.render(cookie)))
    }
    val headers = Headers.from(fields.result())

    // Defer body serialization until Potateh writes the response.
    val bodyLength =
      if length.nonEmpty then length
      else if transferEncoding.nonEmpty then None
      else knownBodyLength
    val body = response.body match {
      case None if length.nonEmpty && (requestMethod == "HEAD" || response.status.code == 304) =>
        ResponseBody.Writer(length)(_ => ())
      case None =>
        require(length.forall(_ == 0), "Nonzero Content-Length without a body")
        ResponseBody.Empty
      case Some(value) => ResponseBody.Writer(bodyLength) { output => response.rw.write(value, output) }
    }
    ba.sake.potateh.Response(response.status.code, headers, body)
  }

  private def isManaged(name: String, connectionFields: Seq[String]): Boolean =
    ManagedHeaders.exists(name.equalsIgnoreCase) ||
      connectionFields.exists(name.equalsIgnoreCase)

}
