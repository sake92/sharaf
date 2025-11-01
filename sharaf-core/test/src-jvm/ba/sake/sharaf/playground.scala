package ba.sake.sharaf

import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.formson.FormDataRW

object Playground {
  val routes = Routes {
    case GET -> Path("hello") =>
      Response.withBody("")
    case GET -> Path("hello") =>
      Response.withBody("Unreachable case")
    case GET -> Path("qp") =>
      case class MyQP(id: Int) derives QueryStringRW
      Request.current.queryParams[MyQP]
      Request.current.queryParamsValidated[MyQP]
      Request.current.queryParams[(id: Int)]
      Request.current.queryParamsValidated[(id: Int)]
      Request.current.queryParams[(id: Int) | (email: String)]
      Request.current.queryParamsValidated[(id: Int) | (email: String)]
      // Request.current.queryParams[String] // not a case class or named tuple
      // Request.current.queryParamsValidated[String] // not a case class or named tuple
      Response.withBody("")
    case GET -> Path("json") =>
      case class MyJson(id: Int) derives JsonRW
      Request.current.bodyJson[MyJson]
      Request.current.bodyJsonValidated[MyJson]
      Request.current.bodyJson[(id: Int)]
      Request.current.bodyJsonValidated[(id: Int)]
      Request.current.bodyJson[(id: Int) | (email: String)]
      Request.current.bodyJsonValidated[(id: Int) | (email: String)]
      Request.current.bodyJson[String] // yes, anything
      Request.current.bodyJson[String] // yes, anything
      Response.withBody("")
    case GET -> Path("form") =>
      case class MyForm(id: Int) derives FormDataRW
      Request.current.bodyForm[MyForm]
      Request.current.bodyFormValidated[MyForm]
      Request.current.bodyForm[(id: Int)]
      Request.current.bodyFormValidated[(id: Int)]
      Request.current.bodyForm[(id: Int) | (email: String)]
      Request.current.bodyFormValidated[(id: Int) | (email: String)]
      // Request.current.bodyForm[String] // not a case class or named tuple
      // Request.current.bodyFormValidated[String] // not a case class or named tuple
      Response.withBody("")
  }
}
