package demo

import org.pac4j.core.client.Clients
import org.pac4j.oauth.client.*

@main def main: Unit = {

  // configure your OAuth2 clients
  // from pac4j's huge list https://www.pac4j.org/docs/clients/oauth.html

  // TODO fill your values here
  // set callback to http://localhost:8080/callback

  val githubClient = new GitHubClient("KEY", "SECRET")
  githubClient.setScope("read:user, user:email")
  // val facebookClient = new FacebookClient(...)

  val clients = new Clients(s"http://localhost:8181/callback", githubClient)

  val server = AppModule(8181, clients).server
  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")
}
