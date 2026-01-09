package ba.sake.sharaf.exceptions

sealed class SharafException(msg: String, cause: Exception = null) extends Exception(msg, cause)

final case class NotFoundException(resource: String) extends SharafException(s"${resource} not found")

final case class RejectedException(msg: String, internalMsg: String = "", cause: Exception = null)
    extends SharafException(msg, cause)

final case class MethodNotAllowedException(msg: String, internalMsg: String = "", cause: Exception = null)
    extends SharafException(msg, cause)

final case class RequestHandlingException(cause: Exception) extends SharafException("Request handling error", cause)
