package ba.sake.sharaf.potateh

import java.net.InetSocketAddress
import java.nio.file.{Files, Path as NioPath}
import scala.util.Using
import scala.concurrent.duration.*
import java.util.concurrent.{CountDownLatch, Executor, Executors, RejectedExecutionException, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import sttp.model.StatusCode
import ba.sake.potateh.{Server, ServerConfig}
import ba.sake.sharaf.*
import ba.sake.tupson.*
import ba.sake.sharaf.handlers.HttpConnection

class PotatehSharafServerTest extends munit.FunSuite {
  private val routes = Routes {
    case GET -> Path("ok")             => Response.withBody("ok")
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

  test("known lengths use UTF-8 bytes and preserve keep-alive framing") {
    withServer() { server =>
      Using.resource(new HttpConnection(server.address)) { connection =>
        for ((path, expected) <- Seq("/length" -> "ok", "/unicode-length" -> "ć", "/ok" -> "ok")) {
          val response = connection.request("GET", path)
          assertEquals(response.status, 200)
          assertEquals(response.body, expected)
          assertEquals(response.headers.get("content-length"), Some("2"))
          assert(!response.headers.contains("transfer-encoding"))
        }
      }
    }
  }

  test("engine-owned and Connection-nominated headers do not leak") {
    withServer() { server =>
      Using.resource(new HttpConnection(server.address)) { connection =>
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

  test("bootstrap rejects unsupported methods and invalid UTF-8 paths with either mapper") {
    for mapper <- Seq(ExceptionMapper.default, ExceptionMapper.json) do {
      withServer(mapper) { server =>
        for scenario <- Seq(
            (method = "FOO", target = "/ok", status = 501),
            (method = "GET", target = "/path/%FF", status = 400),
            (method = "GET", target = "/path/%C0%AF", status = 400),
            (method = "GET", target = "/path/%ED%A0%80", status = 400)
          )
        do
          Using.resource(new HttpConnection(server.address)) { connection =>
            val response = connection.request(scenario.method, scenario.target)
            assertEquals(response.status, scenario.status)
            if mapper == ExceptionMapper.json then
              assertEquals(response.body.parseJson[ba.sake.sharaf.exceptions.ProblemDetails].status, scenario.status)
          }
      }
    }
  }

  test("protocol-level malformed URI returns 400 and closes; server recovers") {
    withServer() { server =>
      for (target <- Seq("/query?a=%ZZ", "/path/%ZZ")) {
        Using.resource(new HttpConnection(server.address)) { connection =>
          val response = connection.request("GET", target)
          assertEquals(response.status, 400)
          assertEquals(response.headers.get("connection"), Some("close"))
        }
      }
      Using.resource(new HttpConnection(server.address))(c => assertEquals(c.request("GET", "/ok").body, "ok"))
    }
  }

  test("HEAD and bodyless status codes preserve the next response") {
    withServer() { server =>
      Using.resource(new HttpConnection(server.address)) { connection =>
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
        Using.resource(new HttpConnection(server.address)) { connection =>
          assertEquals(connection.request("GET", path).status, 500)
        }
      }
      for (path <- Seq("/short", "/long")) {
        Using.resource(new HttpConnection(server.address)) { connection =>
          connection.send("GET", path)
          // A buffered head may never reach the peer when writing fails. Either way, no
          // complete body or second response may follow the mismatched length.
          val wire = connection.remaining()
          assert(wire.isEmpty || wire.startsWith("HTTP/1.1 200"))
          assert(!wire.endsWith("\r\n\r\nok"))
        }
      }
      Using.resource(new HttpConnection(server.address))(c => assertEquals(c.request("GET", "/ok").body, "ok"))
    }
  }

  test("custom mapper sees bootstrap and request decoding failures") {
    import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMethodException}
    val mapper: ExceptionMapper = { case _: BadRequestException | _: UnsupportedMethodException =>
      Response.withBody("custom").withStatus(StatusCode.BadRequest)
    }
    withServer(mapper) { server =>
      Using.resource(new HttpConnection(server.address)) { connection =>
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
      Using.resource(new HttpConnection(server.address)) { connection =>
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
        try Using.resource(new HttpConnection(server.address))(c => assertEquals(c.request("GET", "/ok").body, "ok"))
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

  test("executor rejection closes the admitted connection and the server remains usable") {
    val rejected = new CountDownLatch(1)
    val dispatched = new AtomicInteger()
    val pool = Executors.newSingleThreadExecutor()
    val executor: Executor = task => {
      if dispatched.getAndIncrement() == 0 then {
        rejected.countDown()
        throw new RejectedExecutionException("test rejection")
      } else pool.execute(task)
    }
    try {
      val server = PotatehSharafServer.start(config, SharafHandler.routes(routes), executor)
      try {
        // Native may report the immediate rejected close during connect or read.
        try
          Using.resource(new HttpConnection(server.address)) { connection =>
            val remaining = connection.remaining()
            assertEquals(remaining, "")
          }
        catch case _: java.net.SocketException => ()
        val wasRejected = rejected.await(5, TimeUnit.SECONDS)
        assert(wasRejected, "The server must actually reject an admitted connection")
        Using.resource(new HttpConnection(server.address)) { connection =>
          val response = connection.request("GET", "/ok")
          assertEquals(response.status, 200)
          assertEquals(response.body, "ok")
        }
      } finally server.stop()
    } finally {
      pool.shutdownNow()
      val terminated = pool.awaitTermination(5, TimeUnit.SECONDS)
      assert(terminated)
    }
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
      Using.resource(new HttpConnection(server.address)) { connection =>
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
    val completions = new AtomicInteger()
    val sender = SseSender()
      .withPingInterval(20.millis)
      .onComplete(() => {
        completions.incrementAndGet()
        completed.countDown()
      })
      .onError(_ => { errors.incrementAndGet(); () })
    sender.send(ServerSentEvent.Message("done"))
    sender.send(ServerSentEvent.Done())
    val streaming = Routes { case GET -> Path("events") => Response.withBody(sender) }
    val server = PotatehSharafServer.start(config, streaming)
    try
      Using.resource(new HttpConnection(server.address)) { connection =>
        val response = connection.request("GET", "/events")
        assertEquals(response.status, 200)
        val callbackRan = completed.await(5, TimeUnit.SECONDS)
        assert(callbackRan)
        assertEquals(errors.get(), 0)
        assert(sender._isDone)
        // Pings can be queued behind Done before completion. They must stop accumulating afterwards.
        val queued = sender.queue.size()
        Thread.sleep(100)
        assertEquals(sender.queue.size(), queued)
        assertEquals(completions.get(), 1)
      }
    finally server.stop()
  }

  for scenario <- Seq(
      (name = "HEAD", method = "HEAD", status = 200),
      (name = "204", method = "POST", status = 204),
      (name = "304", method = "POST", status = 304),
      (name = "response conversion failure", method = "POST", status = 500)
    )
  do {
    test(s"upload cleanup runs when the response writer is skipped: ${scenario.name}") {
      val observed = new AtomicReference[NioPath]()
      val attempted = new AtomicBoolean(false)
      val uploadRoutes = Routes { case _ -> Path("upload") =>
        val file = Request.current
          .bodyFormRaw("file")
          .collectFirst { case ba.sake.formson.FormValue.File(path) =>
            path
          }
          .get
        observed.set(file)
        val response = Response.withBody(new java.io.InputStream {
          override def read(): Int = {
            attempted.set(true)
            -1
          }
        })
        if scenario.status == 500 then response.settingHeader("Content-Length", "-2")
        else response.withStatus(StatusCode(scenario.status))
      }
      val server = PotatehSharafServer.start(config, uploadRoutes)
      try {
        Using.resource(new HttpConnection(server.address)) { connection =>
          val body =
            "--boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n\r\nhello\r\n--boundary--\r\n"
          val response = connection.request(scenario.method, "/upload", body, "multipart/form-data; boundary=boundary")
          assertEquals(response.status, scenario.status)
          if scenario.status != 500 then assertEquals(response.body, "")
          assert(!attempted.get(), "The engine should skip the response writer")
          val file = observed.get()
          assert(file != null, "The route must parse the upload")
          val deadline = System.nanoTime() + 5.seconds.toNanos
          while Files.exists(file) && System.nanoTime() < deadline do Thread.sleep(5)
          assert(!Files.exists(file), s"Upload survived response completion: $file")
        }
      } finally server.stop()
    }
  }

}
