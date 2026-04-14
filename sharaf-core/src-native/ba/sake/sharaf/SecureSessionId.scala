package ba.sake.sharaf

import java.io.FileInputStream

private[sharaf] object SecureSessionId {

  def generate(): String = {
    val bytes = new Array[Byte](16) // 128-bit random ID
    val fis = new FileInputStream("/dev/urandom")
    try fis.read(bytes)
    finally fis.close()
    // hex-encode to avoid any Base64 dependency differences on Native
    bytes.map(b => f"${b & 0xff}%02x").mkString
  }
}
