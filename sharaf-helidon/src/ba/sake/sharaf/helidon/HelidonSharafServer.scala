package ba.sake.sharaf.helidon

import io.helidon.config.Config
import io.helidon.webserver.WebServer
import io.helidon.webserver.http.HttpRouting
import ba.sake.sharaf.*

class HelidonSharafServer(host: String, port: Int, sharafHelidonHandler: SharafHelidonHandler) {

  System.setProperty("server.host", host)
  System.setProperty("server.port", port.toString)

  private val server = WebServer
    .builder()
    .config(Config.create().get("server"))
    .routing { (builder: HttpRouting.Builder) =>
      builder.any(sharafHelidonHandler)
      ()
    }
    .build()

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

object HelidonSharafServer {
  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): HelidonSharafServer = {
    val cpResHandler = SharafHandler.classpathResources(
      "public",
      SharafHandler.classpathResources("META-INF/resources/webjars", notFoundHandler)
    )
    val finalHandler =
      SharafHandler.exceptions(
        SharafHandler.cors(
          SharafHandler.routes(routes, cpResHandler),
          corsSettings
        ),
        exceptionMapper
      )
    apply(host, port, finalHandler)
  }

  def apply(host: String, port: Int, sharafHandler: SharafHandler): HelidonSharafServer =
    new HelidonSharafServer(host, port, SharafHelidonHandler(sharafHandler))

  // if need tweaking
  def apply(host: String, port: Int, sharafHelidonHandler: SharafHelidonHandler): HelidonSharafServer =
    new HelidonSharafServer(host, port, sharafHelidonHandler)

}
