package ba.sake.sharaf.handlers

import java.nio.file.{Files, Path as NioPath}
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, Executors, TimeUnit}
import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*
import sttp.model.StatusCode
import ba.sake.sharaf.*
import ba.sake.formson.*
import ba.sake.validson.*
import ba.sake.tupson.*
import ba.sake.sharaf.exceptions.ProblemDetails

/** Uses actual adapter servers and clients, including Native where supported. */
abstract class AbstractMultipartTest extends munit.FunSuite with ServerFixture {
  case class Upload(name: String, file: NioPath) derives FormDataRW
  object Upload {
    given Validator[Upload] = Validator.derived[Upload].minLength(_.name, 3)
  }

  private val binary = (0 to 255).map(_.toByte).toArray ++ "\r\n--upload-boundaryX".getBytes(UTF_8)

  private def body(name: String, payload: Array[Byte]): Array[Byte] =
    s"--upload-boundary\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n$name\r\n--upload-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"binary.bin\"\r\n\r\n"
      .getBytes(UTF_8) ++ payload ++ "\r\n--upload-boundary--\r\n".getBytes(UTF_8)

  private def assertCleaned(observed: AtomicReference[NioPath]): Unit = {
    val path = observed.get()
    assert(path != null, "The handler must actually parse the upload")
    val deadline = System.nanoTime() + 5.seconds.toNanos
    while Files.exists(path) && System.nanoTime() < deadline do Thread.sleep(5)
    assert(!Files.exists(path), s"Upload survived request completion: $path")
  }

  test("concurrent uploads retain their own contents and are cleaned") {
    val arrived = new CountDownLatch(3)
    val observed = new ConcurrentLinkedQueue[NioPath]()
    val routes = Routes { case POST -> Path("upload") =>
      val form = Request.current.bodyForm[Upload]
      observed.add(form.file)
      arrived.countDown()
      if !arrived.await(10, TimeUnit.SECONDS) then throw new IllegalStateException("Uploads did not overlap")
      Response.withBody(s"${form.name}:${new String(Files.readAllBytes(form.file), UTF_8)}")
    }
    withServer(routes, ExceptionMapper.default) { base =>
      val executor = Executors.newFixedThreadPool(3)
      try {
        val results = (1 to 3).map { index =>
          executor.submit(new java.util.concurrent.Callable[(status: Int, body: Array[Byte])] {
            override def call(): (status: Int, body: Array[Byte]) = {
              val result = new AtomicReference[Option[(status: Int, body: Array[Byte])]](None)
              MultipartRequests.withClient(base) { send =>
                result.set(Some(send("upload", body(s"name$index", s"payload$index".getBytes(UTF_8)))))
              }
              result.get().getOrElse(throw new IllegalStateException("Missing upload response"))
            }
          })
        }
        results.zipWithIndex.foreach { (future, index) =>
          val response = future.get(20, TimeUnit.SECONDS)
          assertEquals(response.status, 200)
          assertEquals(new String(response.body, UTF_8), s"name${index + 1}:payload${index + 1}")
        }
        val paths = observed.iterator().asScala.toSeq
        assertEquals(paths.size, 3)
        assertEquals(paths.distinct.size, 3)
        paths.foreach(path => assertCleaned(new AtomicReference(path)))
      } finally {
        executor.shutdownNow()
        executor.awaitTermination(10, TimeUnit.SECONDS)
      }
    }
  }

  test("multipart limits reject excessive input after an uploaded file") {
    val prefix =
      "--upload-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a\"\r\n\r\ndata\r\n"
    val field = "--upload-boundary\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n"
    val end = "\r\n--upload-boundary--\r\n"
    val cases = Seq(
      (name = "field", body = prefix + field + ("x" * (64 * 1024 + 1)) + end),
      (name = "parts", body = prefix + ((field + "x\r\n") * 128) + "--upload-boundary--\r\n"),
      (name = "boundary padding", body = prefix + "--upload-boundary" + (" " * (16 * 1024 + 1)) + "\r\n")
    )
    val routes = Routes { case POST -> Path("upload") =>
      Request.current.bodyForm[Upload]
      Response.default
    }
    withServer(routes, ExceptionMapper.default) { base =>
      MultipartRequests.withClient(base) { send =>
        cases.foreach { scenario =>
          val response = send("upload", scenario.body.getBytes(UTF_8))
          assertEquals(response.status, 413, scenario.name)
        }
      }
    }
  }

  for mapper <- Seq((name = "default", value = ExceptionMapper.default), (name = "json", value = ExceptionMapper.json))
  do {
    for payload <- Seq((name = "binary", bytes = binary), (name = "empty", bytes = Array.emptyByteArray)) do {
      test(s"${mapper.name}: typed ${payload.name} multipart upload is cleaned after success") {
        val observed = new AtomicReference[NioPath]()
        val routes = Routes { case POST -> Path("upload") =>
          val form = Request.current.bodyForm[Upload]
          observed.set(form.file)
          assert(Files.exists(form.file))
          val data = Files.readAllBytes(form.file)
          Response.withBody(s"${form.name}:${data.toSeq.mkString(",")}")
        }
        withServer(routes, mapper.value) { base =>
          MultipartRequests.withClient(base) { send =>
            val response = send("upload", body("hello č", payload.bytes))
            assertEquals(response.status, 200)
            assertEquals(new String(response.body, UTF_8), s"hello č:${payload.bytes.toSeq.mkString(",")}")
            assertCleaned(observed)
          }
        }
      }
    }

    for failure <- Seq((name = "validation", status = 422), (name = "handler", status = 500)) do {
      test(s"${mapper.name}: multipart ${failure.name} failure cleans uploaded files") {
        val observed = new AtomicReference[NioPath]()
        val routes = Routes { case POST -> Path("upload") =>
          observed.set(Request.current.bodyForm[Upload].file)
          if failure.name == "validation" then Request.current.bodyFormValidated[Upload]
          throw new IllegalStateException("test handler failure")
        }
        withServer(routes, mapper.value) { base =>
          MultipartRequests.withClient(base) { send =>
            val response = send("upload", body("", binary))
            assertEquals(response.status, failure.status)
            if mapper.name == "json" then
              assertEquals(new String(response.body, UTF_8).parseJson[ProblemDetails].status, failure.status)
            assertCleaned(observed)
          }
        }
      }
    }

    test(s"${mapper.name}: multipart form parsing failure reports the missing field") {
      val routes = Routes { case POST -> Path("upload") => Response.withBody(Request.current.bodyForm[Upload].name) }
      val missingName =
        "--upload-boundary\r\nContent-Disposition: form-data; name=\"bla\"\r\n\r\n\r\n--upload-boundary--\r\n".getBytes(
          UTF_8
        )
      withServer(routes, mapper.value) { base =>
        MultipartRequests.withClient(base) { send =>
          val response = send("upload", missingName)
          assertEquals(response.status, 400)
          val text = new String(response.body, UTF_8)
          if mapper.name == "json" then {
            val problem = text.parseJson[ProblemDetails]
            assertEquals(problem.status, 400)
            assertEquals(problem.title, "Form parsing errors")
            assert(problem.invalidArguments.exists(arg => arg.path == "name" && arg.reason == "is missing"))
          } else assert(text.contains("Key 'name' is missing"))
        }
      }
    }

    for scenario <- Seq((name = "malformed", status = 400), (name = "oversized", status = 413)) do {
      test(s"${mapper.name}: ${scenario.name} multipart upload is rejected") {
        val routes = Routes { case POST -> Path("upload") => Response.withBody(Request.current.bodyForm[Upload].name) }
        val bytes =
          if scenario.name == "malformed" then body("hello", binary).dropRight(10)
          else body("x" * (64 * 1024 + 1), Array.emptyByteArray)
        withServer(routes, mapper.value) { base =>
          MultipartRequests.withClient(base) { send =>
            val response = send("upload", bytes)
            assertEquals(response.status, scenario.status)
            if mapper.name == "json" then
              assertEquals(new String(response.body, UTF_8).parseJson[ProblemDetails].status, scenario.status)
          }
        }
      }
    }

    test(s"${mapper.name}: uploaded file remains available until the response body is written") {
      val observed = new AtomicReference[NioPath]()
      val routes = Routes { case POST -> Path("upload") =>
        val form = Request.current.bodyForm[Upload]
        observed.set(form.file)
        Response.withBody(form.file)
      }
      withServer(routes, mapper.value) { base =>
        MultipartRequests.withClient(base) { send =>
          val response = send("upload", body("hello", binary))
          assertEquals(response.status, 200)
          assertEquals(response.body.toSeq, binary.toSeq)
          assertCleaned(observed)
        }
      }
    }

    test(s"${mapper.name}: failing response writer cleans uploaded files") {
      val observed = new AtomicReference[NioPath]()
      val attempted = new AtomicBoolean(false)
      val routes = Routes { case POST -> Path("upload") =>
        observed.set(Request.current.bodyForm[Upload].file)
        Response.withBody(new java.io.InputStream {
          override def read(): Int = {
            attempted.set(true)
            throw new java.io.IOException("test response writer failure")
          }
        })
      }
      withServer(routes, mapper.value) { base =>
        MultipartRequests.withClient(base) { send =>
          // Headers may already be sent when the writer fails, so transport failure is valid.
          try send("upload", body("hello", binary))
          catch case _: Exception => ()
          assert(attempted.get(), "The response writer must actually run")
          assertCleaned(observed)
        }
      }
    }

    for status <- Seq(StatusCode.NoContent, StatusCode.NotModified) do {
      test(s"${mapper.name}: bodyless status ${status.code} cleans uploaded files") {
        val observed = new AtomicReference[NioPath]()
        val routes = Routes { case POST -> Path("upload") =>
          observed.set(Request.current.bodyForm[Upload].file)
          Response.withStatus(status)
        }
        withServer(routes, mapper.value) { base =>
          MultipartRequests.withClient(base) { send =>
            val response = send("upload", body("hello", binary))
            assertEquals(response.status, status.code)
            assertEquals(response.body.toSeq, Seq.empty[Byte])
            assertCleaned(observed)
          }
        }
      }
    }
  }
}
