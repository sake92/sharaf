package demo

import scala.util.Random
import io.undertow.Undertow
import ba.sake.formson.*
import ba.sake.tupson.*
import ba.sake.sharaf.Resource

class FormApiSuite extends munit.FunSuite {

  override def munitFixtures = List(serverFixture)

  test("customer can be created") {

    val server = serverFixture()
    val serverInfo = server.getListenerInfo().get(0)
    val baseUrl = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"

    val exampleFile =
      Resource.fromClassPath("example.txt").get.asInstanceOf[Resource.ClasspathResource].underlying.getFile.toPath

    val reqBody =
      CreateCustomerForm("Meho", exampleFile, CreateAddressForm("street123ž"), List("hobby1", "hobby2"))
    val res = requests.post(
      s"$baseUrl/form",
      data = formData2RequestsMultipart(reqBody.toFormDataMap())
    )

    assertEquals(res.statusCode, 200)
    assertEquals(res.headers("content-type"), Seq("application/json")) // it returns JSON content..
    val resBody = res.text.parseJson[CreateCustomerResponse]
    // this tests utf-8 encoding too :)
    assertEquals(resBody.street, "street123ž")
    assertEquals(resBody.fileContents, "This is a text file :)")
  }

  // TODO extract into a separate requests-integration module
  private def formData2RequestsMultipart(formDataMap: FormDataMap) = {
    val multiItems = formDataMap.flatMap { case (key, values) =>
      values.map {
        case FormValue.Str(value)       => requests.MultiItem(key, value)
        case FormValue.File(value)      => requests.MultiItem(key, value, value.getFileName.toString)
        case FormValue.ByteArray(value) => requests.MultiItem(key, value)
      }
    }
    requests.MultiPart(
      multiItems.toSeq*
    )
  }

  val serverFixture = new Fixture[Undertow]("JsonApiServer") {
    private var underlyingServer: Undertow = _
    def apply() = underlyingServer
    override def beforeEach(context: BeforeEach): Unit =
      underlyingServer = FormApiServer(Random.between(1_024, 65_535)).server
      underlyingServer.start()
    override def afterEach(context: AfterEach): Unit =
      underlyingServer.stop
  }

}
