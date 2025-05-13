package userpassform

import scala.compiletime.uninitialized
import ba.sake.sharaf.utils.NetworkUtils

trait IntegrationTest extends munit.FunSuite {

  protected val moduleFixture = new Fixture[UserPassFormModule]("UserPassFormModule") {

    private var module: UserPassFormModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      val port = NetworkUtils.getFreePort()
      module = UserPassFormModule(port)
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

  override def munitFixtures = List(moduleFixture)
}
