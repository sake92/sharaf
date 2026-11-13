package ba.sake.sharaf

import scala.NamedTuple.AnyNamedTuple
import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.validson.*
import ba.sake.sharaf.exceptions.*
import org.typelevel.jawn.ast.JValue

trait Request {

  def headers: Map[HttpString, Seq[String]]

  private[sharaf] def headerValues(name: HttpString): Option[Seq[String]] =
    headers.get(name)

  def cookies: Seq[Cookie] =
    CookieHeaders.parse(headers.getOrElse(HttpString("Cookie"), Seq.empty))

  def queryParamsRaw: QueryStringMap

  // must be a Product (case class)
  def queryParams[T <: Product | AnyNamedTuple: QueryStringRW]: T =
    try queryParamsRaw.parseQueryStringMap
    catch case e: QuersonException => throw RequestHandlingException(e)

  def queryParamsValidated[T <: Product | AnyNamedTuple: QueryStringRW: Validator]: T =
    try queryParams[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  def bodyString: String

  def bodyJsonRaw: JValue = bodyJson[JValue]

  def bodyJson[T: JsonRW]: T =
    try bodyString.parseJson[T]
    catch case e: TupsonException => throw RequestHandlingException(e)

  def bodyJsonValidated[T: JsonRW: Validator]: T =
    try bodyJson[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  def bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    if UrlEncoded.isForm(contentType) then UrlEncoded.form(bodyString)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

  // must be a Product (case class)
  def bodyForm[T <: Product | AnyNamedTuple: FormDataRW]: T =
    try bodyFormRaw.parseFormDataMap[T]
    catch case e: FormsonException => throw RequestHandlingException(e)

  def bodyFormValidated[T <: Product | AnyNamedTuple: FormDataRW: Validator]: T =
    try bodyForm[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

}

object Request {
  def current[Req <: Request](using req: Req): Req = req
}
