package ba.sake.sharaf.exceptions

sealed class SharafException(msg: String, cause: Exception = null) extends Exception(msg, cause)

final case class NotFoundException(resource: String) extends SharafException(s"${resource} not found")

final case class RejectedException(msg: String, internalMsg: String = "", cause: Exception = null)
    extends SharafException(msg, cause)

final case class MethodNotAllowedException(msg: String, internalMsg: String = "", cause: Exception = null)
    extends SharafException(msg, cause)

final case class RequestHandlingException(cause: Exception) extends SharafException("Request handling error", cause)

/** Malformed request syntax or encoding detected by a server adapter. */
final case class BadRequestException(msg: String) extends SharafException(msg)

/** A request body format that the adapter cannot parse. */
final case class UnsupportedMediaTypeException(msg: String) extends SharafException(msg)

/** A valid HTTP method token not implemented by Sharaf. */
final case class UnsupportedMethodException(method: String) extends SharafException("Unsupported HTTP method")

/** A bounded request parser rejected an oversized body or part. */
final case class PayloadTooLargeException(msg: String) extends SharafException(msg)
