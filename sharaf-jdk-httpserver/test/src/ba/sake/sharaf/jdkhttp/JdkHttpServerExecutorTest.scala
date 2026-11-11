package ba.sake.sharaf.jdkhttp

import java.util.concurrent.{Executor, Executors, TimeUnit}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import sttp.client4.quick.*
import ba.sake.sharaf.*

class JdkHttpServerExecutorTest extends munit.FunSuite with JdkHttpServerServerFixture {

  test("a supplied executor handles requests and remains caller-owned") {
    val worker = new AtomicReference[Thread]()
    val executor = Executors.newSingleThreadExecutor()
    val submissions = new AtomicInteger()
    val trackingExecutor: Executor = command => {
      submissions.incrementAndGet()
      executor.execute(command)
    }
    val handler = SharafHandler.routes(Routes {
      case GET -> Path() =>
        worker.set(Thread.currentThread())
        Response.withBody("ok")
    })
    val server = JdkHttpServerSharafServer("127.0.0.1", handler, Some(trackingExecutor))
    try {
      server.start()
      assertEquals(request.get(uri"http://127.0.0.1:${server.port}/").send(httpBackend).body, "ok")
      assert(worker.get() != null)
      assert(submissions.get() > 0)
      server.stop()
      assert(!executor.isShutdown)
      assertEquals(executor.submit(() => 42).get(5, TimeUnit.SECONDS), 42)
    } finally {
      server.stop()
      executor.shutdownNow()
    }
  }

  test("the default executor stops its worker after the server stops") {
    val worker = new AtomicReference[Thread]()
    val handler = SharafHandler.routes(Routes {
      case GET -> Path() =>
        worker.set(Thread.currentThread())
        Response.withBody("ok")
    })
    val server = JdkHttpServerSharafServer("127.0.0.1", handler)
    server.start()
    try {
      assertEquals(request.get(uri"http://127.0.0.1:${server.port}/").send(httpBackend).body, "ok")
    } finally server.stop()
    val thread = worker.get()
    assert(thread != null)
    thread.join(5000)
    assert(!thread.isAlive)
  }
}
