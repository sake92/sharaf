package ba.sake.sharaf.pac4j

import org.pac4j.core.context.WebContext
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.exception.http.*
import ba.sake.sharaf.Response
import sttp.model.StatusCode

/** Converts pac4j [[HttpAction]]s to Sharaf [[Response]] objects. */
final class SharafHttpActionAdapter extends HttpActionAdapter:

  override def adapt(action: HttpAction, context: WebContext): AnyRef =
    val baseResponse = action match
      case _: UnauthorizedAction =>
        Response.withStatus(StatusCode.Unauthorized)
      case _: ForbiddenAction =>
        Response.withStatus(StatusCode.Forbidden)
      case a: OkAction =>
        var res = Response.withStatus(StatusCode.Ok)
        Option(a.getContent).foreach { content =>
          res = res.withBody(content)
        }
        res
      case _: NoContentAction =>
        Response.withStatus(StatusCode.NoContent)
      case _: BadRequestAction =>
        Response.withStatus(StatusCode.BadRequest)
      case a: FoundAction =>
        Response.redirect(a.getLocation).withStatus(StatusCode.Found)
      case a: SeeOtherAction =>
        Response.redirect(a.getLocation).withStatus(StatusCode.SeeOther)
      case a: WithLocationAction =>
        Response.redirect(a.getLocation)
      case a: StatusAction =>
        Response.withStatus(StatusCode.unsafeApply(a.getCode))
      case a: WithContentAction =>
        var res = Response.withStatus(StatusCode.unsafeApply(a.getCode))
        Option(a.getContent).foreach { content =>
          res = res.withBody(content)
        }
        res
      case _ =>
        Response.withStatus(StatusCode.InternalServerError)

    context match
      case sharafCtx: SharafWebContext => sharafCtx.supplementResponse(baseResponse)
      case _ => baseResponse
