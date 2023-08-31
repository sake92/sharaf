package demo

class AppTests extends IntegrationTest {

  test("/protected should return 401 when not logged in") {
    val (_, baseUrl) = moduleFixture()

    val res = requests.get(s"$baseUrl/protected", check = false)

    assertEquals(res.statusCode, 401)
  }

  test("/protected should return 200 when logged in") {
    val (_, baseUrl) = moduleFixture()
    val session = createSession(baseUrl)

    val res = session.get(s"$baseUrl/protected")

    assertEquals(res.statusCode, 200)
  }
}
