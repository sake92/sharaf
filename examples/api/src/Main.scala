package api

import java.util.UUID
import io.undertow.Undertow
import ba.sake.sharaf.*,  routing.*

@main def main: Unit =
  val module = JsonApiModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class JsonApiModule(port: Int) {

  val baseUrl = s"http://localhost:${port}"

  // don't do this at home!
  private var db = Seq.empty[ProductRes]

  private val routes = Routes:
    case GET() -> Path("products", param[UUID](id)) =>
      val productOpt = db.find(_.id == id)
      Response.withBodyOpt(productOpt, s"Product with id=$id")

    case GET() -> Path("products") =>
      val query = Request.current.queryParamsValidated[ProductsQuery]
      val products =
        if query.name.isEmpty then db
        else db.filter(c => query.name.contains(c.name) && query.minQuantity.map(c.quantity >= _).getOrElse(true))
      Response.withBody(products.toList)

    case POST() -> Path("products") =>
      val req = Request.current.bodyJsonValidated[CreateProductReq]
      val res = ProductRes(UUID.randomUUID(), req.name, req.quantity)
      db = db.appended(res)
      Response.withBody(res)

  private val handler = SharafHandler(routes)
    .withExceptionMapper(ExceptionMapper.json)

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(handler)
    .build()
}
