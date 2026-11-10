package ba.sake.sharaf.handlers

import java.net.{InetSocketAddress, URI}
import java.nio.charset.StandardCharsets.{ISO_8859_1, UTF_8}
import scala.util.Using
import sttp.client4.quick.*
import sttp.model.StatusCode
import ba.sake.sharaf.*

abstract class AbstractUrlEncodedLimitTest extends munit.FunSuite with ServerFixture {
  private val routes = Routes {
    case POST -> Path("form") => Response.withBody(Request.current.bodyFormRaw("x").size.toString)
    case POST -> Path("body") => Response.withBody(Request.current.bodyString.length.toString)
  }

  test("URL-encoded forms accept the limit and reject a larger fixed-length body") {
    withServer(routes, ExceptionMapper.default) { base =>
      val atLimit = "x=" + ("a" * (UrlEncoded.maxFormBytes - 2))
      val accepted = request
        .post(uri"$base/body")
        .contentType("application/x-www-form-urlencoded")
        .body(atLimit)
        .send(httpBackend)
      assertEquals(accepted.code, StatusCode.Ok)
      assertEquals(accepted.body, UrlEncoded.maxFormBytes.toString)
      val parsed = request
        .post(uri"$base/form")
        .contentType("application/x-www-form-urlencoded")
        .body(atLimit)
        .send(httpBackend)
      assertEquals(parsed.code, StatusCode.Ok)
      assertEquals(parsed.body, "1")
      val rejected = request
        .post(uri"$base/form")
        .contentType("application/x-www-form-urlencoded")
        .body(atLimit + "a")
        .send(httpBackend)
      assertEquals(rejected.code, StatusCode.PayloadTooLarge)
    }
  }

  test("URL-encoded chunked bodies are rejected before the final chunk") {
    withServer(routes, ExceptionMapper.default) { base =>
      val uri = URI.create(base)
      Using.resource(new HttpConnection(new InetSocketAddress(uri.getHost, uri.getPort))) { connection =>
        val output = connection.socket.getOutputStream
        val bytes = ("x=" + ("a" * (UrlEncoded.maxFormBytes - 1))).getBytes(UTF_8)
        val headers =
          s"POST /form HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/x-www-form-urlencoded\r\nTransfer-Encoding: chunked\r\n\r\n${bytes.length.toHexString}\r\n"
        output.write(headers.getBytes(ISO_8859_1))
        output.write(bytes)
        output.write("\r\n".getBytes(ISO_8859_1))
        output.flush()
        val response = connection.readResponse()
        assertEquals(response.status, 413)
        val remaining = connection.remaining()
        assert(remaining.isEmpty || remaining == "URL-encoded form limit exceeded")
      }
    }
  }
}
