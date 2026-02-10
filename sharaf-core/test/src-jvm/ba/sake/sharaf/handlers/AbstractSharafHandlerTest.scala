package ba.sake.sharaf.handlers

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

abstract class AbstractSharafHandlerTest extends munit.FunSuite {

  val port: Int = NetworkUtils.getFreePort()
  def baseUrl: String = s"http://localhost:${port}"

  def startServer(): Unit
  def stopServer(): Unit

  override def beforeAll(): Unit = startServer()
  override def afterAll(): Unit = stopServer()

  val routes = Routes { case GET -> Path("hello") =>
    Response.withBody("hello")
  }

  test("/does-not-exist returns a 404") {
    println(s"Testing 404 on ${baseUrl}/does-not-exist")
    val res = quickRequest.get(uri"${baseUrl}/does-not-exist").send()
    assertEquals(res.code, StatusCode.NotFound)
    assertEquals(res.body, "Not Found")
  }

  test("/hello returns a string") {
    val res = quickRequest.get(uri"${baseUrl}/hello").send()
    assertEquals(res.body, "hello")
  }
}