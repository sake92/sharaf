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

  val module = AppModule(8181, clients)
  module.server.start()

  println(s"Started HTTP server at ${module.baseUrl}")
}
