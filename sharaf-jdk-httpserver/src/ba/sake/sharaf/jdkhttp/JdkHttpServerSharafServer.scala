package ba.sake.sharaf.jdkhttp

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.sun.net.httpserver.{HttpServer, HttpHandler}
import ba.sake.sharaf.*
import sttp.model.StatusCode

class JdkHttpServerSharafServer(host: String, port: Int, handler: HttpHandler) {

  private val server = HttpServer.create(new InetSocketAddress(host, port), 0)
  server.createContext("/", handler)
  // Use a fixed thread pool executor for handling requests
  server.setExecutor(Executors.newFixedThreadPool(10))

  def start(): Unit = server.start()

  def stop(): Unit = server.stop(0)
}

object JdkHttpServerSharafServer {

  private val defaultNotFoundResponse = Response.withBody("Not Found").withStatus(StatusCode.NotFound)

  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: Request => Response[?] = _ => defaultNotFoundResponse
  ): JdkHttpServerSharafServer = {
    val notFoundRoutes = Routes { _ =>
      notFoundHandler(Request.current)
    }
    val finalHandler = JdkHttpExceptionHandler(
      exceptionMapper,
      next = SharafJdkHttpHandler(
        SharafHandler.cors(
          corsSettings,
          SharafHandler.routes(routes)
        ),
        next = Some(
          SharafJdkHttpHandler(SharafHandler.routes(notFoundRoutes))
        )
      )
    )
    new JdkHttpServerSharafServer(host, port, finalHandler)
  }

  def apply(host: String, port: Int, sharafHandler: SharafHandler): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, SharafJdkHttpHandler(sharafHandler))

  def apply(host: String, port: Int, sharafJdkHttpHandler: SharafJdkHttpHandler): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, sharafJdkHttpHandler)
}
