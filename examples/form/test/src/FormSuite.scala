package demo

import ba.sake.formson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.*
import java.nio.file.Path

class FormSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("Customer can be created") {

    val module = moduleFixture()

    val exampleFile = Path.of(getClass.getClassLoader.getResource("example.txt").toURI())

    val reqBody =
      CreateCustomerForm("Meho", exampleFile, CreateAddressForm("street123ž"), List("hobby1", "hobby2"))
    val res = requests.post(
      s"${module.baseUrl}/form-submit",
      data = reqBody.toFormDataMap().toRequestsMultipart()
    )

    assertEquals(res.statusCode, 200)
    val resBody = res.text()
    // this tests utf-8 encoding too :)
    assert(resBody.contains("street123ž"), "Result does not contain input street")
    assert(resBody.contains("This is a text file :)"), "Result does not contain input file")
  }

  val moduleFixture = new Fixture[FormModule]("FormModule") {
    private var module: FormModule = _

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = FormModule(getFreePort())
      module.server.start()
    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
