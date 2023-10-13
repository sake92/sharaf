package ba.sake.sharaf

class SharafException(msg: String) extends Exception(msg)

class NotFoundException(val resource: String) extends Exception(s"$resource not found")
