package jwt

import scala.compiletime.uninitialized
import ba.sake.sharaf.utils.NetworkUtils

trait IntegrationTest extends munit.FunSuite {

  protected val moduleFixture = new Fixture[JwtModule]("JwtModule") {

    private var module: JwtModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      val port = NetworkUtils.getFreePort()
      module = JwtModule(port)
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

  override def munitFixtures = List(moduleFixture)
}
