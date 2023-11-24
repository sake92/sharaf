package ba.sake.sharaf

sealed class SharafException(msg: String, cause: Exception = null) extends Exception(msg, cause)

class NotFoundException(val resource: String) extends SharafException(s"$resource not found")

class RequestHandlingException(cause: Exception) extends SharafException("Request handling error", cause)
