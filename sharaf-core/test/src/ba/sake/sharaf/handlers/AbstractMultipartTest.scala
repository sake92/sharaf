package ba.sake.sharaf.handlers

import java.nio.file.{Files, Path as NioPath}
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.concurrent.duration.*
import ba.sake.sharaf.*
import ba.sake.formson.*
import ba.sake.validson.*
import ba.sake.tupson.*

/** Uses actual adapter servers and clients, including Native where supported. */
abstract class AbstractMultipartTest extends munit.FunSuite {
  def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit
  def supportsDeferredUploadResponse: Boolean = true

  case class Upload(name: String, file: NioPath) derives FormDataRW
  object Upload {
    given Validator[Upload] = Validator.derived[Upload].minLength(_.name, 3)
  }

  for mapper <- Seq((name = "default", value = ExceptionMapper.default), (name = "json", value = ExceptionMapper.json))
  do {
    test(s"${mapper.name}: typed binary and empty multipart uploads, validation, errors and cleanup") {
      val observed = new AtomicReference[NioPath]()
      val writerAttempted = new AtomicBoolean(false)
      val binary = (0 to 255).map(_.toByte).toArray ++ "\r\n--upload-boundaryX".getBytes(UTF_8)
      val routes = Routes {
        case POST -> Path("upload") =>
          val form = Request.current.bodyForm[Upload]
          observed.set(form.file)
          assert(Files.exists(form.file))
          val data = Files.readAllBytes(form.file)
          Response.withBody(s"${form.name}:${data.toSeq.mkString(",")}")
        case POST -> Path("validate") =>
          val raw = Request.current.bodyForm[Upload]
          observed.set(raw.file)
          val form = Request.current.bodyFormValidated[Upload]
          Response.withBody(form.name)
        case POST -> Path("failure") =>
          observed.set(Request.current.bodyForm[Upload].file)
          throw new IllegalStateException("test handler failure")
        case POST -> Path("deferred") =>
          val form = Request.current.bodyForm[Upload]
          observed.set(form.file)
          Response.withBody(form.file)
        case POST -> Path("writer-failure") =>
          val form = Request.current.bodyForm[Upload]
          observed.set(form.file)
          Response.withBody(new java.io.InputStream {
            override def read(): Int = {
              writerAttempted.set(true)
              throw new java.io.IOException("test response writer failure")
            }
          })
      }
      def body(name: String, payload: Array[Byte]): Array[Byte] =
        s"--upload-boundary\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n$name\r\n--upload-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"binary.bin\"\r\n\r\n"
          .getBytes(UTF_8) ++ payload ++ "\r\n--upload-boundary--\r\n".getBytes(UTF_8)
      def assertCleaned(): Unit = {
        val deadline = System.nanoTime() + 5.seconds.toNanos
        while Files.exists(observed.get()) && System.nanoTime() < deadline do Thread.sleep(5)
        assert(!Files.exists(observed.get()), s"Upload survived request completion: ${observed.get()}")
      }
      withServer(routes, mapper.value) { base =>
        MultipartRequests.withClient(base) { send =>
          def text(bytes: Array[Byte]): String = new String(bytes, UTF_8)
          for payload <- Seq(binary, Array.emptyByteArray) do {
            val response = send("upload", body("hello č", payload))
            assertEquals(response.status, 200)
            assertEquals(text(response.body), s"hello č:${payload.toSeq.mkString(",")}")
            assertCleaned()
          }
          val invalid = send("validate", body("", binary))
          assertEquals(invalid.status, 422)
          assertCleaned()
          val failure = send("failure", body("hello", binary))
          assertEquals(failure.status, 500)
          assertCleaned()
          val malformed = send("upload", body("hello", binary).dropRight(10))
          assertEquals(malformed.status, 400)
          if mapper.name == "json" then
            assertEquals(text(malformed.body).parseJson[ba.sake.sharaf.exceptions.ProblemDetails].status, 400)
          val oversized = send("upload", body("x" * (64 * 1024 + 1), Array.emptyByteArray))
          assertEquals(oversized.status, 413)
          if mapper.name == "json" then
            assertEquals(text(oversized.body).parseJson[ba.sake.sharaf.exceptions.ProblemDetails].status, 413)
          if supportsDeferredUploadResponse then {
            val response = send("deferred", body("hello", binary))
            assertEquals(response.status, 200)
            assertEquals(response.body.toSeq, binary.toSeq)
            assertCleaned()
            // A failing writer can abort HTTP after headers; either a response or a transport failure is valid.
            try send("writer-failure", body("hello", binary))
            catch case _: Exception => ()
            assert(writerAttempted.get(), "The response writer must actually run")
            assertCleaned()
          }
        }
      }
    }
  }
}
