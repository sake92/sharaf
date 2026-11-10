package ba.sake.sharaf.potateh

import java.io.{BufferedInputStream, ByteArrayOutputStream}
import java.net.{InetSocketAddress, Socket}
import java.nio.charset.StandardCharsets.{UTF_8, ISO_8859_1}
import scala.util.Using
import scala.concurrent.duration.*
import java.util.concurrent.{CountDownLatch, Executor, Executors, RejectedExecutionException, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import sttp.model.StatusCode
import sttp.client4.quick.*
import ba.sake.potateh.{Server, ServerConfig}
import ba.sake.sharaf.*
import ba.sake.tupson.*
import ba.sake.querson.*
import ba.sake.formson.*

class PotatehSharafServerTest extends munit.FunSuite {
  case class Params(x: String) derives QueryStringRW, FormDataRW
  case class Message(value: String) derives JsonRW
  case class JsonItem(id: Int, text: String, tags: Seq[String]) derives JsonRW
  case class JsonPayload(items: Seq[JsonItem], count: Int) derives JsonRW

  private val routes = Routes {
    case GET -> Path("ok")             => Response.withBody("ok")
    case GET -> Path("query")          => Response.withBody(Request.current.queryParamsRaw("a").mkString("|"))
    case GET -> Path("typed-query")    => Response.withBody(Request.current.queryParams[Params].x)
    case POST -> Path("form")          => Response.withBody(Request.current.bodyFormRaw("x").mkString("|"))
    case POST -> Path("typed-form")    => Response.withBody(Request.current.bodyForm[Params].x)
    case POST -> Path("json")          => Response.withBody(Request.current.bodyJson[Message])
    case GET -> Path("fail")           => throw new IllegalArgumentException("private application error")
    case GET -> Path("path", value)    => Response.withBody(value)
    case GET -> Path("length")         => Response.withBody("ok").settingHeader("Content-Length", "2")
    case GET -> Path("unicode-length") => Response.withBody("ć").settingHeader("Content-Length", "2")
    case HEAD -> Path("head")          => Response.withBody("ok").settingHeader("Content-Length", "2")
    case GET -> Path("status", code)   => Response.withBody("ok").withStatus(StatusCode(code.toInt))
    case GET -> Path("short")          => Response.withBody("ok").settingHeader("Content-Length", "3")
    case GET -> Path("long")           => Response.withBody("ok").settingHeader("Content-Length", "1")
    case HEAD -> Path("metadata")      => Response.settingHeader("Content-Length", "42")
    case GET -> Path("metadata") => Response.withStatus(StatusCode.NotModified).settingHeader("Content-Length", "42")
    case GET -> Path("conflict") =>
      Response
        .withBody("ok")
        .settingHeader("Content-Length", "2")
        .settingHeader("Transfer-Encoding", "chunked")
    case GET -> Path("close")      => Response.withBody("ok").settingHeader("Connection", "close")
    case GET -> Path("bad-length") => Response.withBody("ok").settingHeader("Content-Length", "-2")
    case GET -> Path("sse") =>
      val sender = SseSender()
      sender.send(ServerSentEvent.Message("hello"))
      sender.send(ServerSentEvent.Done())
      Response.withBody(sender)
    case GET -> Path("hop") =>
      Response
        .withBody("ok")
        .settingHeader("Connection", "keep-alive, X-Private")
        .settingHeader("Keep-Alive", "timeout=1")
        .settingHeader("X-Private", "secret")
        .settingHeader("Transfer-Encoding", "chunked")
        .settingHeader("X-Public", "yes")
        .settingCookie(Cookie("token", "value"))
  }

  private val config = ServerConfig.Default.withBindAddress(new InetSocketAddress("127.0.0.1", 0))
  private def withServer[A](mapper: ExceptionMapper = ExceptionMapper.default)(f: Server => A): A = {
    val server = PotatehSharafServer.start(config, routes, exceptionMapper = mapper)
    try f(server)
    finally server.stop()
  }

  test("real adapter handles a normal request through sttp") {
    withServer() { server =>
      val response = quickRequest
        .get(uri"http://127.0.0.1:${server.address.getPort}/ok")
        .httpVersion(sttp.model.HttpVersion.HTTP_1_1)
        .send()
      assertEquals(response.code.code, 200)
      assertEquals(response.body, "ok")
    }
  }

  test("explicit lengths use UTF-8 bytes and preserve keep-alive framing") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        for ((path, expected) <- Seq("/length" -> "ok", "/unicode-length" -> "ć", "/ok" -> "ok")) {
          val response = connection.request("GET", path)
          assertEquals(response.status, 200)
          assertEquals(response.body, expected)
          if path != "/ok" then assertEquals(response.headers.get("content-length"), Some("2"))
        }
      }
    }
  }

  test("SSE delivers events and completes") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        val response = connection.request("GET", "/sse")
        assertEquals(response.status, 200)
        assertEquals(response.headers.get("content-type"), Some("text/event-stream"))
        assertEquals(response.body, "data: hello\n\nevent: stop\ndata:\n\n")
      }
    }
  }

  test("engine-owned and Connection-nominated headers do not leak") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        val response = connection.request("GET", "/hop")
        assertEquals(response.status, 200)
        assertEquals(response.body, "ok")
        assertEquals(response.headers.get("x-public"), Some("yes"))
        assertEquals(response.headers.get("set-cookie"), Some("token=value"))
        assert(!response.headers.contains("x-private"))
        assert(!response.headers.contains("keep-alive"))
      }
    }
  }

  for ((name, mapper) <- Seq("text" -> ExceptionMapper.default, "json" -> ExceptionMapper.json)) {
    test(s"$name mapper handles malformed request data and preserves server errors") {
      withServer(mapper) { server =>
        for (
          (method, path, body, contentType, status) <- Seq(
            ("GET", "/query?a=%FF", "", "", 200),
            ("GET", "/typed-query?x=%FF", "", "", 200),
            ("POST", "/form", "x=%ZZ", "application/x-www-form-urlencoded", 400),
            ("POST", "/typed-form", "x=%FF", "application/x-www-form-urlencoded", 200),
            ("POST", "/form", "x=ok", "multipart/form-data; boundary=test", 400),
            ("POST", "/typed-form", "x=ok", "text/plain", 415),
            ("POST", "/json", "{", "application/json", 400),
            ("FOO", "/ok", "", "", 501),
            ("GET", "/fail", "", "", 500),
            ("GET", "/ok", "", "", 200)
          )
        ) {
          Using.resource(new Connection(server)) { connection =>
            val response = connection.request(method, path, body, contentType)
            assertEquals(response.status, status, s"$method $path $body")
            assert(!response.body.contains("private application error"))
            if name == "json" && status != 200 then {
              val problem = response.body.parseJson[ba.sake.sharaf.exceptions.ProblemDetails]
              assertEquals(problem.status, status)
            }
          }
        }
      }
    }
  }

  test("query decoding preserves repetition, empty values, plus and Unicode") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        val response = connection.request("GET", "/query?a=one+two&a=&a=%C4%87&a=%252F")
        assertEquals(response.status, 200)
        assertEquals(response.body, "one two||ć|%2F")
      }
    }
  }

  test("query and form UTF-8 use replacement while malformed escapes still fail") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        assertEquals(connection.request("GET", "/typed-query?x=%FF").body, "�")
        val form =
          connection.request("POST", "/typed-form", "x=%FF", "Application/X-WWW-Form-Urlencoded; charset=UTF-8")
        assertEquals(form.status, 200)
        assertEquals(form.body, "�")
      }
    }
  }

  test("paths split before strict decoding and retain plus and single decoding") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        for ((path, value) <- Seq("a%2Fb" -> "a/b", "a+b" -> "a+b", "%252F" -> "%2F", "%C4%87" -> "ć")) {
          val response = connection.request("GET", s"/path/$path")
          assertEquals(response.status, 200)
          assertEquals(response.body, value)
        }
        for (path <- Seq("%FF", "%C0%AF", "%ED%A0%80")) {
          assertEquals(connection.request("GET", s"/path/$path").status, 400)
        }
      }
    }
  }

  test("protocol-level malformed URI returns 400 and closes; server recovers") {
    withServer() { server =>
      for (target <- Seq("/query?a=%ZZ", "/path/%ZZ")) {
        Using.resource(new Connection(server)) { connection =>
          val response = connection.request("GET", target)
          assertEquals(response.status, 400)
          assertEquals(response.headers.get("connection"), Some("close"))
        }
      }
      Using.resource(new Connection(server))(c => assertEquals(c.request("GET", "/ok").body, "ok"))
    }
  }

  test("HEAD and bodyless status codes preserve the next response") {
    withServer() { server =>
      Using.resource(new Connection(server)) { connection =>
        val head = connection.request("HEAD", "/head")
        assertEquals(head.status, 200)
        assertEquals(head.headers.get("content-length"), Some("2"))
        assertEquals(head.body, "")
        for (method <- Seq("HEAD", "GET")) {
          val metadata = connection.request(method, "/metadata")
          assertEquals(metadata.status, if method == "HEAD" then 200 else 304)
          assertEquals(metadata.headers.get("content-length"), Some("42"))
          assertEquals(metadata.body, "")
        }
        for (status <- Seq(204, 205, 304)) {
          val response = connection.request("GET", s"/status/$status")
          assertEquals(response.status, status)
          assertEquals(response.body, "")
          assert(!response.headers.contains("transfer-encoding"))
        }
        assertEquals(connection.request("GET", "/ok").body, "ok")
      }
    }
  }

  test("invalid or contradictory framing fails safely before the body") {
    withServer() { server =>
      for (path <- Seq("/bad-length", "/conflict", "/close")) {
        Using.resource(new Connection(server)) { connection =>
          assertEquals(connection.request("GET", path).status, 500)
        }
      }
      for (path <- Seq("/short", "/long")) {
        Using.resource(new Connection(server)) { connection =>
          connection.send("GET", path)
          // A buffered head may never reach the peer when writing fails. Either way, no
          // complete body or second response may follow the mismatched length.
          val wire = connection.remaining()
          assert(wire.isEmpty || wire.startsWith("HTTP/1.1 200"))
          assert(!wire.endsWith("\r\n\r\nok"))
        }
      }
      Using.resource(new Connection(server))(c => assertEquals(c.request("GET", "/ok").body, "ok"))
    }
  }

  test("custom mapper sees bootstrap and request decoding failures") {
    import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMethodException}
    val mapper: ExceptionMapper = { case _: BadRequestException | _: UnsupportedMethodException =>
      Response.withBody("custom").withStatus(StatusCode.BadRequest)
    }
    withServer(mapper) { server =>
      Using.resource(new Connection(server)) { connection =>
        for ((method, target) <- Seq("FOO" -> "/ok", "GET" -> "/path/%FF")) {
          val response = connection.request(method, target)
          assertEquals(response.status, 400)
          assertEquals(response.body, "custom")
        }
      }
    }
  }

  test("composed handler uses default mapper for bootstrap failures") {
    val server = PotatehSharafServer.start(config, SharafHandler.routes(routes))
    try
      Using.resource(new Connection(server)) { connection =>
        assertEquals(connection.request("FOO", "/ok").status, 501)
        assertEquals(connection.request("GET", "/path/%FF").status, 400)
        assertEquals(connection.request("GET", "/ok").body, "ok")
      }
    finally server.stop()
  }

  test("caller executor dispatches connections and remains usable after stop") {
    val pool = Executors.newCachedThreadPool()
    val dispatched = new AtomicInteger()
    val executor: Executor = task => {
      dispatched.incrementAndGet()
      pool.execute(task)
    }
    try {
      for (useRoutes <- Seq(true, false)) {
        val server =
          if useRoutes then PotatehSharafServer.start(config, routes, executor = executor)
          else PotatehSharafServer.start(config, SharafHandler.routes(routes), executor)
        try Using.resource(new Connection(server))(c => assertEquals(c.request("GET", "/ok").body, "ok"))
        finally server.stop()
      }
      assertEquals(dispatched.get(), 2)
      val stillOpen = new CountDownLatch(1)
      pool.execute(() => stillOpen.countDown())
      assert(stillOpen.await(5, TimeUnit.SECONDS))
    } finally {
      pool.shutdownNow()
      val terminated = pool.awaitTermination(5, TimeUnit.SECONDS)
      assert(terminated)
    }
  }

  test("executor rejection closes the admitted connection") {
    val rejected = new CountDownLatch(1)
    val executor: Executor = _ => {
      rejected.countDown()
      throw new RejectedExecutionException("test rejection")
    }
    val server = PotatehSharafServer.start(config, SharafHandler.routes(routes), executor)
    try
      Using.resource(new Connection(server)) { connection =>
        assert(rejected.await(5, TimeUnit.SECONDS))
        assertEquals(connection.remaining(), "")
      }
    finally server.stop()
  }

  test("SSE flushes before completion and stops pinging after disconnect") {
    val errors = new CountDownLatch(1)
    val completed = new AtomicInteger()
    val sender = SseSender()
      .withPingInterval(20.millis)
      .onError(_ => errors.countDown())
      .onComplete(() => {
        completed.incrementAndGet()
        ()
      })
    val streaming = Routes { case GET -> Path("events") =>
      sender.send(ServerSentEvent.Message("live"))
      Response.withBody(sender)
    }
    val server = PotatehSharafServer.start(config, streaming)
    try {
      Using.resource(new Connection(server)) { connection =>
        connection.send("GET", "/events")
        assertEquals(connection.line().split(" ")(1), "200")
        var line = connection.line()
        while line.nonEmpty do line = connection.line()
        val size = Integer.parseInt(connection.line(), 16)
        assertEquals(connection.readBytes(size), "data: live\n\n")
        connection.socket.setSoLinger(true, 0)
      }
      assert(errors.await(5, TimeUnit.SECONDS), "disconnect did not finish the SSE writer")
      assertEquals(completed.get(), 0)
      assert(sender._isDone)
      val queued = sender.queue.size()
      Thread.sleep(100)
      assertEquals(sender.queue.size(), queued, "ping producer outlived the stream")
    } finally server.stop()
  }

  test("SSE completion callback runs once and stops the ping producer") {
    val completed = new CountDownLatch(1)
    val errors = new AtomicInteger()
    val sender = SseSender()
      .withPingInterval(20.millis)
      .onComplete(() => completed.countDown())
      .onError(_ => { errors.incrementAndGet(); () })
    sender.send(ServerSentEvent.Message("done"))
    sender.send(ServerSentEvent.Done())
    val streaming = Routes { case GET -> Path("events") => Response.withBody(sender) }
    val server = PotatehSharafServer.start(config, streaming)
    try
      Using.resource(new Connection(server)) { connection =>
        assertEquals(connection.request("GET", "/events").status, 200)
        assert(completed.await(5, TimeUnit.SECONDS))
        assertEquals(errors.get(), 0)
        assert(sender._isDone)
        assertEquals(sender.queue.size(), 0)
        Thread.sleep(100)
        assertEquals(sender.queue.size(), 0)
      }
    finally server.stop()
  }

  test("concurrent JSON responses retain large nested payloads") {
    val items = (0 until 50).map(i => JsonItem(i, "ćž\n\"\\" * 40, Seq("one", "two")))
    val expected = JsonPayload(items, 50)
    val jsonRoutes = Routes { case GET -> Path("json") => Response.withBody(expected) }
    val server = PotatehSharafServer.start(config, jsonRoutes)
    val pool = Executors.newFixedThreadPool(32)
    val finished = new CountDownLatch(32)
    val failures = new java.util.concurrent.ConcurrentLinkedQueue[Throwable]()
    try {
      for (_ <- 0 until 32) pool.execute(() => {
        try
          Using.resource(new Connection(server)) { connection =>
            for (_ <- 0 until 10) {
              val response = connection.request("GET", "/json")
              assertEquals(response.status, 200)
              assertEquals(response.body.parseJson[JsonPayload], expected)
            }
          }
        catch {
          case scala.util.control.NonFatal(error) =>
            failures.add(error)
            ()
        } finally finished.countDown()
      })
      // MUnit evaluates assert's by-name argument while holding the suite monitor.
      // Wait outside it so client workers can make their own assertions.
      val allFinished = finished.await(20, TimeUnit.SECONDS)
      assert(allFinished, "concurrent requests did not finish")
      if !failures.isEmpty then throw failures.peek()
    } finally {
      server.stop()
      pool.shutdownNow()
      val terminated = pool.awaitTermination(5, TimeUnit.SECONDS)
      assert(terminated)
    }
  }

  private case class WireResponse(status: Int, headers: Map[String, String], body: String)

  // Raw sockets preserve malformed targets and expose framing hidden by HTTP clients.
  private class Connection(server: Server) extends AutoCloseable {
    val socket = new Socket()
    socket.connect(server.address, 5000)
    socket.setSoTimeout(5000)
    private val input = new BufferedInputStream(socket.getInputStream)
    private val output = socket.getOutputStream

    def request(method: String, target: String, body: String = "", contentType: String = ""): WireResponse = {
      send(method, target, body, contentType)
      readResponse(method == "HEAD")
    }

    def send(method: String, target: String, body: String = "", contentType: String = ""): Unit = {
      val bytes = body.getBytes(UTF_8)
      val contentHeader = if contentType.isEmpty then "" else s"Content-Type: $contentType\r\n"
      output.write(
        s"$method $target HTTP/1.1\r\nHost: localhost\r\nContent-Length: ${bytes.length}\r\n$contentHeader\r\n"
          .getBytes(ISO_8859_1)
      )
      if bytes.nonEmpty then output.write(bytes)
      output.flush()
    }

    def remaining(): String = new String(input.readAllBytes(), UTF_8)
    def readBytes(size: Int): String = new String(input.readNBytes(size), UTF_8)

    def line(): String = {
      val result = new ByteArrayOutputStream()
      var next = input.read()
      while next != '\n' && next != -1 do {
        if next != '\r' then result.write(next)
        next = input.read()
      }
      assert(next != -1, "unexpected connection close")
      result.toString("ISO-8859-1")
    }

    def readResponse(head: Boolean = false): WireResponse = {
      val status = line().split(" ")(1).toInt
      val headers = Map.newBuilder[String, String]
      var header = line()
      while header.nonEmpty do {
        val parts = header.split(":", 2)
        headers += parts(0).toLowerCase(java.util.Locale.ROOT) -> parts(1).trim
        header = line()
      }
      val fields = headers.result()
      val body = new ByteArrayOutputStream()
      if !head && !Set(204, 205, 304).contains(status) then {
        if fields.get("transfer-encoding").contains("chunked") then {
          var size = Integer.parseInt(line().takeWhile(_ != ';'), 16)
          while size != 0 do {
            body.write(input.readNBytes(size))
            assertEquals(line(), "")
            size = Integer.parseInt(line(), 16)
          }
          assertEquals(line(), "")
        } else body.write(input.readNBytes(fields.getOrElse("content-length", "0").toInt))
      }
      WireResponse(status, fields, body.toString("UTF-8"))
    }

    override def close(): Unit = socket.close()
  }
}
