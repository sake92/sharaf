package ba.sake.sharaf.jdkhttp

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.sun.net.httpserver.{HttpServer, HttpHandler}
import ba.sake.sharaf.*

class JdkHttpServerSharafServer(host: String, listenPort: Int, handler: HttpHandler) {

  private val server = HttpServer.create(new InetSocketAddress(host, listenPort), 0)
  server.createContext("/", handler)
  // Use a fixed thread pool executor for handling requests
  // TODO make configurable executor
  server.setExecutor(Executors.newFixedThreadPool(10))

  /** The bound port; available after the server starts. */
  def port: Int = server.getAddress.getPort

  def start(): Unit = server.start()

  def stop(): Unit = server.stop(0)
}

object JdkHttpServerSharafServer {

  def apply(host: String, handler: SharafHandler): JdkHttpServerSharafServer = apply(host, 0, handler)

  def apply(host: String, routes: Routes): JdkHttpServerSharafServer = apply(host, 0, routes)

  def apply(host: String, handler: SharafJdkHttpHandler): JdkHttpServerSharafServer = apply(host, 0, handler)

  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): JdkHttpServerSharafServer = {
    val cpResHandler = SharafHandler.classpathResources(
      "public",
      SharafHandler.classpathResources("META-INF/resources/webjars", notFoundHandler)
    )
    val finalHandler = SharafHandler.exceptions(
      SharafHandler.cors(
        SharafHandler.routes(routes, cpResHandler),
        corsSettings
      ),
      exceptionMapper
    )
    new JdkHttpServerSharafServer(host, port, SharafJdkHttpHandler(finalHandler))
  }

  def apply(host: String, port: Int, sharafHandler: SharafHandler): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, SharafJdkHttpHandler(sharafHandler))

  def apply(host: String, port: Int, sharafJdkHttpHandler: SharafJdkHttpHandler): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, sharafJdkHttpHandler)
}
