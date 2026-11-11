package ba.sake.sharaf.jdkhttp

import java.net.InetSocketAddress
import java.util.concurrent.{Executor, Executors}
import com.sun.net.httpserver.{HttpServer, HttpHandler}
import ba.sake.sharaf.*

class JdkHttpServerSharafServer(host: String, listenPort: Int, handler: HttpHandler, executor: Option[Executor]) {

  def this(host: String, listenPort: Int, handler: HttpHandler) = this(host, listenPort, handler, None)

  private val server = HttpServer.create(new InetSocketAddress(host, listenPort), 0)
  server.createContext("/", handler)
  private val defaultExecutor = Option.when(executor.isEmpty)(Executors.newFixedThreadPool(10))
  server.setExecutor(executor.getOrElse(defaultExecutor.get))

  /** The bound port; available after the server starts. */
  def port: Int = server.getAddress.getPort

  def start(): Unit = server.start()

  def stop(): Unit =
    try server.stop(0)
    finally defaultExecutor.foreach(_.shutdownNow())
}

object JdkHttpServerSharafServer {

  def apply(host: String, handler: SharafHandler): JdkHttpServerSharafServer = apply(host, 0, handler)

  def apply(host: String, routes: Routes): JdkHttpServerSharafServer = apply(host, 0, routes)

  def apply(host: String, handler: SharafJdkHttpHandler): JdkHttpServerSharafServer = apply(host, 0, handler)

  def apply(host: String, handler: SharafHandler, executor: Option[Executor]): JdkHttpServerSharafServer =
    apply(host, 0, handler, executor)

  def apply(host: String, routes: Routes, executor: Option[Executor]): JdkHttpServerSharafServer =
    apply(host, 0, routes, executor)

  def apply(host: String, handler: SharafJdkHttpHandler, executor: Option[Executor]): JdkHttpServerSharafServer =
    apply(host, 0, handler, executor)

  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): JdkHttpServerSharafServer =
    apply(host, port, routes, corsSettings, exceptionMapper, notFoundHandler, None)

  def apply(host: String, port: Int, routes: Routes, executor: Option[Executor]): JdkHttpServerSharafServer =
    apply(
      host, port, routes, CorsSettings.default, ExceptionMapper.default, SharafHandler.DefaultNotFoundHandler, executor
    )

  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings,
      exceptionMapper: ExceptionMapper,
      notFoundHandler: SharafHandler,
      executor: Option[Executor]
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
    new JdkHttpServerSharafServer(host, port, SharafJdkHttpHandler(finalHandler), executor)
  }

  def apply(host: String, port: Int, sharafHandler: SharafHandler): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, SharafJdkHttpHandler(sharafHandler))

  def apply(
      host: String,
      port: Int,
      sharafHandler: SharafHandler,
      executor: Option[Executor]
  ): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, SharafJdkHttpHandler(sharafHandler), executor)

  def apply(host: String, port: Int, sharafJdkHttpHandler: SharafJdkHttpHandler): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, sharafJdkHttpHandler)

  def apply(
      host: String,
      port: Int,
      sharafJdkHttpHandler: SharafJdkHttpHandler,
      executor: Option[Executor]
  ): JdkHttpServerSharafServer =
    new JdkHttpServerSharafServer(host, port, sharafJdkHttpHandler, executor)
}
