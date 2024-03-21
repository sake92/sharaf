package ba.sake.sharaf.exceptions

sealed class SharafException(msg: String, cause: Exception = null) extends Exception(msg, cause)

final class NotFoundException(val resource: String) extends SharafException(s"$resource not found")

final class RequestHandlingException(cause: Exception) extends SharafException("Request handling error", cause)
