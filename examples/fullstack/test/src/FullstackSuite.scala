package fullstack

import java.nio.file.Path
import scala.compiletime.uninitialized
import sttp.model.*
import sttp.client4.quick.*
import ba.sake.formson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.*

class FullstackSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("Customer can be created") {

    val module = moduleFixture()

    val exampleFile = Path.of(getClass.getClassLoader.getResource("example.txt").toURI())

    val reqBody =
      CreateCustomerForm("Džemal", exampleFile, List("hobby1", "hobby2"))
    val res = quickRequest.post(uri"${module.baseUrl}/form-submit").multipartBody(reqBody.toSttpMultipart()).send()

    assertEquals(res.code, StatusCode.Ok)
    val resBody = res.body
    // this tests utf-8 encoding too :)
    assert(resBody.contains("Džemal"), "Result does not contain input name")
    assert(resBody.contains("This is a text file :)"), "Result does not contain input file")
  }

  val moduleFixture = new Fixture[FullstackModule]("FullstackModule") {
    private var module: FullstackModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = FullstackModule(NetworkUtils.getFreePort())
      module.server.start()
    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
