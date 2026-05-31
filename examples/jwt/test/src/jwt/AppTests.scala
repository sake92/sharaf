package jwt

import sttp.model.*
import sttp.client4.quick.*
import org.pac4j.core.profile.BasicUserProfile
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator

class AppTests extends IntegrationTest {

  private val jwtSecret = "your_jwt_secret_key_that_is_at_least_32_chars"

  private def generateTestJwt(userId: String = "12345"): String =
    val sigConfig = SecretSignatureConfiguration(jwtSecret)
    val up = BasicUserProfile()
    up.setId(userId)
    JwtGenerator(sigConfig).generate(up)

  test("GET / should return 200 OK on public route") {
    val module = moduleFixture()
    val res = quickRequest.get(uri"${module.baseUrl}/").send()
    assertEquals(res.code, StatusCode.Ok)
    assert(res.body.contains("public endpoint"))
  }

  test("GET /protected should return 401 Unauthorized without JWT") {
    val module = moduleFixture()
    val res = quickRequest.get(uri"${module.baseUrl}/protected").send()
    assertEquals(res.code, StatusCode.Unauthorized)
  }

  test("GET /protected should return 200 OK with valid JWT") {
    val module = moduleFixture()
    val jwt = generateTestJwt()
    val res = quickRequest
      .get(uri"${module.baseUrl}/protected")
      .header("Authorization", jwt)
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assert(res.body.contains("protected resource"))
  }

  test("GET /protected/whoami should return user ID from JWT") {
    val module = moduleFixture()
    val jwt = generateTestJwt("12345")
    val res = quickRequest
      .get(uri"${module.baseUrl}/protected/whoami")
      .header("Authorization", jwt)
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assert(res.body.contains("12345"))
  }
}
