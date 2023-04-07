package ba.sake.sharaf

class SharafException(msg: String) extends Exception(msg)

class ValidationException(msg: String) extends SharafException(msg)
