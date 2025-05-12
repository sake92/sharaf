package ba.sake.sharaf

import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.validson.*
import ba.sake.sharaf.exceptions.*
import org.typelevel.jawn.ast.JValue

trait Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]]

  def cookies: Seq[Cookie]

  /* *** QUERY *** */
  def queryParamsRaw: QueryStringMap

  // must be a Product (case class)
  def queryParams[T <: Product: QueryStringRW]: T =
    try queryParamsRaw.parseQueryStringMap
    catch case e: QuersonException => throw RequestHandlingException(e)

  def queryParamsValidated[T <: Product: QueryStringRW: Validator]: T =
    try queryParams[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  /* *** BODY *** */
  def bodyString: String

  // JSON
  def bodyJsonRaw: JValue = bodyJson[JValue]

  def bodyJson[T: JsonRW]: T =
    try bodyString.parseJson[T]
    catch case e: TupsonException => throw RequestHandlingException(e)

  def bodyJsonValidated[T: JsonRW: Validator]: T =
    try bodyJson[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  // FORM
  def bodyFormRaw: FormDataMap

  // must be a Product (case class)
  def bodyForm[T <: Product: FormDataRW]: T =
    try bodyFormRaw.parseFormDataMap[T]
    catch case e: FormsonException => throw RequestHandlingException(e)

  def bodyFormValidated[T <: Product: FormDataRW: Validator]: T =
    try bodyForm[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

}

object Request {
  def current[Req <: Request](using req: Req): Req = req
}
