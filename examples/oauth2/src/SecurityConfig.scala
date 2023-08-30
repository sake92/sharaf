package demo

import scala.jdk.CollectionConverters.*

import org.pac4j.core.client.Clients
import org.pac4j.core.matching.matcher.*
import org.pac4j.core.config.Config

class SecurityConfig(clients: Clients) {

  private val publicRoutesMatcherName = "publicRoutesMatcher"

  val matchers = Set(
    DefaultMatchers.SECURITYHEADERS,
    publicRoutesMatcherName
  ).mkString(",")

  val pac4jConfig = {

    val publicRoutesMatcher = new PathMatcher()
    // exclude fixed paths
    publicRoutesMatcher.excludePaths("/")
    // exclude glob stuff* paths
    Seq("/js", "/images").foreach(publicRoutesMatcher.excludeBranch)

    val config = new Config()
    config.setClients(clients)
    config.addMatcher(publicRoutesMatcherName, publicRoutesMatcher)
    config
  }

  val clientNames = pac4jConfig.getClients().getClients().asScala.map(_.getName()).toSeq
}
