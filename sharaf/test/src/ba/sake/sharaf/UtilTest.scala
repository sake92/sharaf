package ba.sake.sharaf

import java.net.URL
import ba.sake.sharaf.utils.parse
import ba.sake.tupson.JsonRW
import com.typesafe.config.ConfigFactory
import ba.sake.tupson.discriminator

class UtilTest extends munit.FunSuite {

  test("conf parse normal") {
    val config = ConfigFactory.load("test1").parse[Test1Conf]()
    assertEquals(
      config,
      Test1Conf(
        TestConf(
          7777,
          URL("http://example.com"),
          "str",
          Seq("a", "b", "c"),
          TestConfPoly.Poly2(123)
        )
      )
    )
  }
  test("conf parse overriden by sys prop") {
    System.setProperty("sysprop.port", "1234")
    ConfigFactory.invalidateCaches()
    val config = ConfigFactory.load("test_sys_prop").parse[TestSysPropConf]()
    assertEquals(
      config,
      TestSysPropConf(
        TestConf(
          1234,
          URL("http://example.com"),
          "str",
          Seq("a", "b", "c"),
          TestConfPoly.Poly2(123)
        )
      )
    )
  }
  test("conf parse overriden by env var") {
    val config = ConfigFactory.load("test_env_var").parse[TestEnvVarConf]()
    assertEquals(
      config,
      TestEnvVarConf(
        TestConf(
          1234,
          URL("http://example.com"),
          "str",
          Seq("a", "b", "c"),
          TestConfPoly.Poly2(123)
        )
      )
    )
  }

}

case class Test1Conf(
    test1: TestConf
) derives JsonRW

case class TestSysPropConf(
    sysprop: TestConf
) derives JsonRW

case class TestEnvVarConf(
    envvar: TestConf
) derives JsonRW

case class TestConf(
    port: Int,
    url: URL,
    string: String,
    seq: Seq[String],
    poly: TestConfPoly
) derives JsonRW

@discriminator("what")
enum TestConfPoly derives JsonRW:
  case Poly1()
  case Poly2(x: Int)
