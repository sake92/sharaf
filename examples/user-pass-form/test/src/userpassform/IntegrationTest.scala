package userpassform

import ba.sake.sharaf.utils.*

import scala.compiletime.uninitialized

trait IntegrationTest extends munit.FunSuite {

  protected val moduleFixture = new Fixture[UserPassFormModule]("UserPassFormModule") {

    private var module: UserPassFormModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      val port = getFreePort()
      module = UserPassFormModule(port)
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

  override def munitFixtures = List(moduleFixture)
}
