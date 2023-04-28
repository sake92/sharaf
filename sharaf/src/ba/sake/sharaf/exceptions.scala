package ba.sake.sharaf

class SharafException(msg: String) extends Exception(msg)

class NotFoundException(val name: String) extends Exception(s"$name not found")
