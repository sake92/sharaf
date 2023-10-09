package demo

import ba.sake.formson.*
import ba.sake.tupson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.*

class FormApiSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("Customer can be created") {

    val module = moduleFixture()

    val exampleFile =
      Resource.fromClassPath("example.txt").get.asInstanceOf[Resource.ClasspathResource].underlying.getFile.toPath

    val reqBody =
      CreateCustomerForm("Meho", exampleFile, CreateAddressForm("street123ž"), List("hobby1", "hobby2"))
    val res = requests.post(
      s"${module.baseUrl}/form",
      data = reqBody.toFormDataMap().toRequestsMultipart()
    )

    assertEquals(res.statusCode, 200)
    val resBody = res.text.parseJson[CreateCustomerResponse]
    // this tests utf-8 encoding too :)
    assertEquals(resBody.street, "street123ž")
    assertEquals(resBody.fileContents, "This is a text file :)")
  }

  val moduleFixture = new Fixture[FormApiModule]("FormApiModule") {
    private var module: FormApiModule = _

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = FormApiModule(getFreePort())
      module.server.start()
    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
