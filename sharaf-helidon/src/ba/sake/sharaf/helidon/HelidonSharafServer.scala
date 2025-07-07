package ba.sake.sharaf.helidon

import io.helidon.config.Config
import io.helidon.webserver.WebServer
import io.helidon.webserver.http.HttpRouting
import ba.sake.sharaf.SharafHandler

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

  def apply(host: String, port: Int, sharafHandler: SharafHandler): HelidonSharafServer =
    new HelidonSharafServer(host, port, SharafHelidonHandler(sharafHandler))

  // if need tweaking
  def apply(host: String, port: Int, sharafHelidonHandler: SharafHelidonHandler): HelidonSharafServer =
    new HelidonSharafServer(host, port, sharafHelidonHandler)

}
