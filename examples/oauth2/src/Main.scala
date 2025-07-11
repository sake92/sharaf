package demo

import org.pac4j.core.client.Clients
import org.pac4j.oauth.client.*
import ba.sake.sharaf.utils.NetworkUtils

@main def main(): Unit =
  System.setProperty("org.jboss.logging.provider", "slf4j")
  // configure your OAuth2 clients with your values
  // from pac4j's huge list https://www.pac4j.org/docs/clients/oauth.html
  val githubClient = GitHubClient("KEY", "SECRET")
  githubClient.setScope("read:user, user:email")
  val port = NetworkUtils.getFreePort()
  val clients = Clients(s"http://localhost:${port}/callback", githubClient)
  val module = AppModule(port, clients)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")
