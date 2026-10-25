//> using test.dep org.scalameta::munit:1.3.6

package authorization

import sttp.client4.quick.*
import sttp.model.Method
import ba.sake.tupson.*
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.sharaf.utils.NetworkUtils
import scala.concurrent.duration.*

abstract class HttpExampleSuite extends munit.FunSuite:
  protected final def withServer(make: Int => UndertowSharafServer)(check: String => Unit): Unit =
    val port = NetworkUtils.getFreePort()
    val server = make(port)
    try
      server.start()
      check(s"http://localhost:$port")
    finally server.stop()

  protected final def request(
      base: String,
      path: String,
      method: String = "GET",
      user: Option[String] = None,
      body: String = "",
      password: String = "demo"
  ): sttp.client4.Response[String] =
    val url = base + path
    val req = quickRequest.method(Method.unsafeApply(method), uri"$url")
      .contentType("application/json").body(body).readTimeout(5.seconds)
    val authenticated = user.fold(req)(name => req.auth.basic(name, password))
    authenticated.send()

  protected final def decoded[T: JsonRW](response: sttp.client4.Response[String]): T =
    assertEquals(response.code.code, 200)
    response.body.parseJson[T]
