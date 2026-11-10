package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import sttp.model.HeaderNames
import ba.sake.sharaf.{*, given}

abstract class AbstractTwirlResponseTest extends munit.FunSuite with ServerFixture {
  private val routes = Routes {
    case GET -> Path("twirl", "html") =>
      Response.withBody(html"""
        <html>
          <head>
            <title>Twirl HTML</title>
          </head>
          <body>
            <h1>This is a Twirl HTML response</h1>
          </body>
        </html>
      """)
    case GET -> Path("twirl", "xml") =>
      Response.withBody(xml"""
        <?xml version="1.0" encoding="UTF-8"?>
        <note>
        <to>Tove</to>
        <from>Jani</from>
        <heading>Reminder</heading>
        <body>Don't forget me this weekend!</body>
        </note>
      """)
  }

  test("Write response twirl HTML") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val res = request.get(uri"${baseUrl}/twirl/html").send(httpBackend)
      assertEquals(
        res.body.trim,
        """
        <html>
          <head>
            <title>Twirl HTML</title>
          </head>
          <body>
            <h1>This is a Twirl HTML response</h1>
          </body>
        </html> """.trim
      )
      assertEquals(res.headers(HeaderNames.ContentType), Seq("text/html; charset=utf-8"))
    }
  }

  test("Write response twirl XML") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val res = request.get(uri"${baseUrl}/twirl/xml").send(httpBackend)
      assertEquals(
        res.body.trim,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <note>
        <to>Tove</to>
        <from>Jani</from>
        <heading>Reminder</heading>
        <body>Don't forget me this weekend!</body>
        </note>
     """.trim
      )
      assertEquals(res.headers(HeaderNames.ContentType), Seq("application/xml; charset=utf-8"))
    }
  }

}
