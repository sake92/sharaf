package ba.sake.sharaf.session.jdbc

import java.io.FileInputStream

private[jdbc] object SecureJdbcSessionId:

  def generate(): String =
    val bytes = new Array[Byte](16)
    val randomBytes = new FileInputStream("/dev/urandom")
    try randomBytes.read(bytes)
    finally randomBytes.close()
    bytes.map(byte => f"${byte & 0xff}%02x").mkString
