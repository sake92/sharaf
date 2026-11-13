package ba.sake.sharaf

import java.nio.charset.StandardCharsets.UTF_8

class KnownBodyLengthTest extends munit.FunSuite {

  test("computes UTF-8 byte length for strings") {
    val highSurrogate = "a" + 0xd800.toChar + "b"
    val lowSurrogate = "a" + 0xdc00.toChar + "b"
    val samples = Seq("", "ok", "café", "€", "😀", highSurrogate, lowSurrogate)
    samples.foreach { value =>
      assertEquals(KnownBodyLength.of(value), Some(value.getBytes(UTF_8).length.toLong))
    }
  }

  test("uses byte-array length and ignores unknown bodies") {
    assertEquals(KnownBodyLength.of(Array[Byte](1, 2, 3)), Some(3L))
    assertEquals(KnownBodyLength.of(42), None)
  }
}
