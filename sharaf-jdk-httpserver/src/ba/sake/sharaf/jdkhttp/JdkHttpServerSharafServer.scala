package ba.sake.sharaf.jdkhttp

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.sun.net.httpserver.{HttpServer, HttpHandler}
import ba.sake.sharaf.*

class JdkHttpServerSharafServer(host: String, port: Int, handler: HttpHandler) {

  private val server = HttpServer.create(new InetSocketAddress(host, port), 0)
  server.createContext("/", handler)
  // Use a fixed thread pool executor for handling requests
  server.setExecutor(Executors.newFixedThreadPool(10))

  def start(): Unit = server.start()

  def stop(): Unit = server.stop(0)
}

object JdkHttpServerSharafServer {

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
