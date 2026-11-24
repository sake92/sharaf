package ba.sake.sharaf

import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
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
  final def queryParams[T <: Product | AnyNamedTuple: QueryStringRW]: T =
    try queryParamsRaw.parseQueryStringMap
    catch case e: QuersonException => throw RequestHandlingException(e)

  final def queryParamsValidated[T <: Product | AnyNamedTuple: QueryStringRW: Validator]: T =
    try queryParams[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  /** The single-use request body, after HTTP framing has been decoded.
    * Consume it within this request and use it instead of the text, JSON or form readers.
    * Application-defined request implementations must override this method to support binary reads.
    */
  def bodyInputStream: InputStream =
    throw new UnsupportedOperationException("This request implementation does not expose a body input stream")

  private lazy val bodyContentType: String =
    headerValues(Request.ContentTypeHeader).flatMap(_.headOption).getOrElse("")

  private[sharaf] def readUrlEncodedBody(input: InputStream): String =
    UrlEncoded.readFormBody(input)

  private lazy val decodedBodyString: String = {
    val input = bodyInputStream
    if UrlEncoded.isForm(bodyContentType) then readUrlEncodedBody(input)
    else new String(input.readAllBytes(), UTF_8)
  }

  def bodyString: String = decodedBodyString

  def bodyJsonRaw: JValue = bodyJson[JValue]

  final def bodyJson[T: JsonRW]: T =
    try bodyString.parseJson[T]
    catch case e: TupsonException => throw RequestHandlingException(e)

  final def bodyJsonValidated[T: JsonRW: Validator]: T =
    try bodyJson[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  private[sharaf] def readMultipartForm(contentType: String): FormDataMap =
    throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

  private lazy val decodedBodyForm: FormDataMap = {
    val contentType = bodyContentType
    if Multipart.isMultipart(contentType) then readMultipartForm(contentType)
    else if UrlEncoded.isForm(contentType) then UrlEncoded.form(bodyString)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")
  }

  def bodyFormRaw: FormDataMap = decodedBodyForm

  // must be a Product (case class)
  final def bodyForm[T <: Product | AnyNamedTuple: FormDataRW]: T =
    try bodyFormRaw.parseFormDataMap[T]
    catch case e: FormsonException => throw RequestHandlingException(e)

  final def bodyFormValidated[T <: Product | AnyNamedTuple: FormDataRW: Validator]: T =
    try bodyForm[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

}

/** Enables multipart parsing for adapters that clean up uploads after response serialization. */
private[sharaf] trait MultipartRequest extends Request {
  private var multipartUploads: Option[MultipartUploads] = None

  private[sharaf] final def closeUploads(): Unit = multipartUploads.foreach(_.close())

  private[sharaf] override def readMultipartForm(contentType: String): FormDataMap =
    parseMultipartForm(bodyInputStream, contentType)

  private[sharaf] final def parseMultipartForm(input: InputStream, contentType: String): FormDataMap = {
    val uploads = multipartUploads.getOrElse {
      val created = new MultipartUploads
      multipartUploads = Some(created)
      created
    }
    Multipart.parse(input, contentType, uploads)
  }
}

object Request {
  private val ContentTypeHeader = HttpString("Content-Type")

  def current[Req <: Request](using req: Req): Req = req
}
