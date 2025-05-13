package demo

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.pac4j.core.client.Clients
import org.pac4j.oauth.client.GenericOAuth20Client
import org.pac4j.core.profile.definition.CommonProfileDefinition
import ba.sake.sharaf.utils.*

object TestData {
  val username = "testUser"
}

trait IntegrationTest extends munit.FunSuite {

  protected val moduleFixture = new Fixture[AppModule]("AppModule") {

    private var mockOauth2server: MockOAuth2Server = uninitialized

    private var module: AppModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =

      // mock OAuth2 server
      mockOauth2server = MockOAuth2Server()
      mockOauth2server.start()

      val issuerId = "fakeOAuthIssuer"

      // set user that gets logged in
      mockOauth2server.enqueueCallback(
        DefaultOAuth2TokenCallback(
          issuerId,
          TestData.username,
          JOSEObjectType.JWT.getType(),
          null,
          Map(
            "id" -> "123",
            "username" -> TestData.username,
            CommonProfileDefinition.DISPLAY_NAME -> TestData.username
          ).asJava
        )
      )

      // start real server
      val client = GenericOAuth20Client()
      client.setKey("fakeKey")
      client.setSecret("fakeSecret")
      client.setAuthUrl(mockOauth2server.authorizationEndpointUrl(issuerId).toString())
      client.setScope("openid whatever")
      client.setTokenUrl(mockOauth2server.tokenEndpointUrl(issuerId).toString())
      client.setProfileUrl(mockOauth2server.userInfoUrl(issuerId).toString())

      val port = NetworkUtils.getFreePort()
      val clients = Clients(s"http://localhost:${port}/callback", client)

      // assign fixture
      module = AppModule(port, clients)
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
      mockOauth2server.shutdown()
  }

  override def munitFixtures = List(moduleFixture)
}
