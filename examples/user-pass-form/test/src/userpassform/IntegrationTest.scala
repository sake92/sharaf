package userpassform

import scala.compiletime.uninitialized

trait IntegrationTest extends munit.FunSuite {

  protected val moduleFixture = new Fixture[UserPassFormModule]("UserPassFormModule") {

    private var module: UserPassFormModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = UserPassFormModule(0)
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

  override def munitFixtures = List(moduleFixture)
}
