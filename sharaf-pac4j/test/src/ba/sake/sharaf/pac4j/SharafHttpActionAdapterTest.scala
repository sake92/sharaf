package ba.sake.sharaf.pac4j

import org.pac4j.core.exception.http.*
import sttp.model.StatusCode

class SharafHttpActionAdapterTest extends munit.FunSuite:

  val adapter = new SharafHttpActionAdapter()

  test("ForbiddenAction maps to 403"):
    val res = adapter.adapt(new ForbiddenAction(), null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.Forbidden)

  test("UnauthorizedAction maps to 401"):
    val res = adapter.adapt(new UnauthorizedAction(), null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.Unauthorized)

  test("OkAction maps to 200 with body"):
    val action = new OkAction("hello")
    val res = adapter.adapt(action, null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.Ok)

  test("FoundAction maps to 302 redirect"):
    val action = new FoundAction("/target")
    val res = adapter.adapt(action, null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.Found)

  test("SeeOtherAction maps to 303 redirect"):
    val action = new SeeOtherAction("/target")
    val res = adapter.adapt(action, null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.SeeOther)

  test("BadRequestAction maps to 400"):
    val res = adapter.adapt(new BadRequestAction(), null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.BadRequest)

  test("StatusAction preserves status code"):
    val action = new StatusAction(429)
    val res = adapter.adapt(action, null).asInstanceOf[ba.sake.sharaf.Response[?]]
    assertEquals(res.status, StatusCode.TooManyRequests)
