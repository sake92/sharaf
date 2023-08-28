package demo

import io.undertow.Undertow

import ba.sake.formson.*
import ba.sake.tupson.*
import ba.sake.sharaf.*

class FormApiSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("customer can be created") {

    val module = moduleFixture()
    val serverInfo = module.server.getListenerInfo().get(0)
    val baseUrl = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"

    val exampleFile =
      Resource.fromClassPath("example.txt").get.asInstanceOf[Resource.ClasspathResource].underlying.getFile.toPath

    val reqBody =
      CreateCustomerForm("Meho", exampleFile, CreateAddressForm("street123ž"), List("hobby1", "hobby2"))
    val res = requests.post(
      s"$baseUrl/form",
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
      module = FormApiModule(SharafUtils.getFreePort())
      module.server.start()
    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
