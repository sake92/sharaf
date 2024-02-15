package fullstack

import scala.compiletime.uninitialized
import ba.sake.formson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.*
import java.nio.file.Path

class FullstackSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("Customer can be created") {

    val module = moduleFixture()

    val exampleFile = Path.of(getClass.getClassLoader.getResource("example.txt").toURI())

    val reqBody =
      CreateCustomerForm("Džemal", exampleFile, List("hobby1", "hobby2"))
    val res = requests.post(
      s"${module.baseUrl}/form-submit",
      data = reqBody.toRequestsMultipart()
    )

    assertEquals(res.statusCode, 200)
    val resBody = res.text()
    // this tests utf-8 encoding too :)
    assert(resBody.contains("Džemal"), "Result does not contain input name")
    assert(resBody.contains("This is a text file :)"), "Result does not contain input file")
  }

  val moduleFixture = new Fixture[FullstackModule]("FullstackModule") {
    private var module: FullstackModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = FullstackModule(getFreePort())
      module.server.start()
    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
