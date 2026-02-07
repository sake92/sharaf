package ba.sake.sharaf.handlers

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*

abstract class AbstractSharafHandlerTest extends munit.FunSuite {

  def port: Int
  def baseUrl: String = s"http://localhost:$port"

  // Abstract method to start the server - implementations must provide this
  def startServer(): Unit

  // Abstract method to stop the server - implementations must provide this
  def stopServer(): Unit

  val routes = Routes { case GET -> Path("hello") =>
    Response.withBody("hello")
  }

  override def beforeAll(): Unit = startServer()

  override def afterAll(): Unit = stopServer()

  test("/does-not-exist returns a 404") {
    val res = quickRequest.get(uri"${baseUrl}/does-not-exist").send()
    assertEquals(res.code, StatusCode.NotFound)
    assertEquals(res.body, "Not Found")
  }

  test("/hello returns a string") {
    val res = quickRequest.get(uri"${baseUrl}/hello").send()
    assertEquals(res.body, "hello")
  }
}