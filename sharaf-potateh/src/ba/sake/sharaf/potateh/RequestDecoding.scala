package ba.sake.sharaf.potateh

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction}
import java.nio.charset.StandardCharsets.UTF_8
import ba.sake.sharaf.exceptions.BadRequestException

private[potateh] object RequestDecoding {
  def utf8(bytes: Array[Byte]): String =
    try
      UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString
    catch case _: CharacterCodingException => throw BadRequestException("Invalid UTF-8 in request")

  def component(value: String, plusAsSpace: Boolean): String = {
    val bytes = new ByteArrayOutputStream()
    var index = 0
    while index < value.length do {
      value.charAt(index) match {
        case '%' =>
          if index + 2 >= value.length then throw BadRequestException("Invalid percent escape in request")
          val hi = hex(value.charAt(index + 1))
          val lo = hex(value.charAt(index + 2))
          if hi < 0 || lo < 0 then throw BadRequestException("Invalid percent escape in request")
          bytes.write(hi * 16 + lo)
          index += 3
        case '+' if plusAsSpace =>
          bytes.write(' ')
          index += 1
        case _ =>
          val end = value.indexOf('%', index) match {
            case -1 => value.length
            case n  => n
          }
          val literal = value.substring(index, end)
          val normalized = if plusAsSpace then literal.replace('+', ' ') else literal
          bytes.write(normalized.getBytes(UTF_8))
          index = end
      }
    }
    utf8(bytes.toByteArray)
  }

  private def hex(char: Char): Int = char match {
    case c if c >= '0' && c <= '9' => c - '0'
    case c if c >= 'a' && c <= 'f' => c - 'a' + 10
    case c if c >= 'A' && c <= 'F' => c - 'A' + 10
    case _                         => -1
  }
}
