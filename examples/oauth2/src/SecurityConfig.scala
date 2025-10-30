package demo

import scala.jdk.CollectionConverters.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.matching.matcher.*

class SecurityConfig(clients: Clients) {

  private val publicRoutesMatcherName = "publicRoutesMatcher"

  val matchers = Set(
    DefaultMatchers.SECURITYHEADERS,
    publicRoutesMatcherName
  ).mkString(",")

  val pac4jConfig = {
    val publicRoutesMatcher = PathMatcher()
    // exclude fixed paths
    publicRoutesMatcher.excludePaths("/")
    // exclude glob stuff* paths
    Seq("/js", "/images").foreach(publicRoutesMatcher.excludeBranch)

    val config = Config(clients)
    config.addMatcher(publicRoutesMatcherName, publicRoutesMatcher)
    config.setCallbackLogic(CustomCallbackLogic())
    config
  }

  val clientNames = clients.getClients.asScala.map(_.getName()).toSeq
}
