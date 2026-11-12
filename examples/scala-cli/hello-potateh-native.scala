// scala --power package hello-potateh-native.scala --server=false --native-mode release-size \
//   --native-multithreading -o /tmp/sharaf-potateh
// /tmp/sharaf-potateh
// curl http://localhost:8080/hello/Scala
//> using scala 3.9.0
//> using platform native
//> using nativeVersion 0.5.12
//> using dep ba.sake::sharaf-potateh::0.21.0

import java.net.InetSocketAddress
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer
import ba.sake.tupson.JsonRW
import sttp.model.StatusCode

case class Message(text: String) derives JsonRW
case class Search(q: Seq[String]) derives ba.sake.querson.QueryStringRW
case class Form(name: String, tags: Seq[String]) derives ba.sake.formson.FormDataRW

object PotatehExample {
  def main(args: Array[String]): Unit = {
    val port = args.headOption.map(_.toInt).getOrElse(8080)
    val routes = Routes {
      case GET -> Path("hello", name) =>
        Response.withBody(s"Hello, $name!")
      case GET -> Path("query") =>
        Response.withBody(Request.current.queryParams[Search].q.mkString("|"))
      case POST -> Path("json") =>
        val message = Request.current.bodyJson[Message]
        Response.withBody(Message(s"Received: ${message.text}"))
      case POST -> Path("form") =>
        val form = Request.current.bodyForm[Form]
        val tags = form.tags.mkString("|")
        Response.withBody(s"${form.name}:$tags")
      case POST -> Path("echo") =>
        Response.withBody(Request.current.bodyString)
      case GET -> Path("headers") =>
        val values = Request.current.headers(HttpString("x-demo")).mkString("|")
        val cookies = Request.current.cookies.map(c => s"${c.name}=${c.value}").mkString("|")
        Response
          .withBody(s"$values;$cookies")
          .settingHeader("X-Repeat", Seq("one", "two"))
          .settingHeader("X-Removed", "gone")
          .removingHeader("X-Removed")
          .settingHeader("Content-Type", "text/custom")
          .settingCookie(Cookie("first", "one", path = Some("/"), httpOnly = true, sameSiteMode = Some("Lax")))
          .settingCookie(Cookie("second", "two"))
          .removingCookie("old")
      case GET -> Path("stream") =>
        Response.withBody(new ByteArrayInputStream("streamed čćž".getBytes(UTF_8)))
      case GET -> Path("empty") => Response.withStatus(StatusCode.NoContent)
      case GET -> Path("fail")  => throw new IllegalStateException("example failure")
    }
    val server = PotatehSharafServer.start(ServerConfig.Default.withBindAddress(new InetSocketAddress("127.0.0.1", port)), routes)
    val hook = new Thread(() => server.stop())
    Runtime.getRuntime.addShutdownHook(hook)
    println(s"LISTENING ${server.address.getPort}")
    System.out.flush()
    try System.in.read()
    finally {
      server.stop()
      try Runtime.getRuntime.removeShutdownHook(hook)
      catch { case _: IllegalStateException => () }
    }
  }
}
