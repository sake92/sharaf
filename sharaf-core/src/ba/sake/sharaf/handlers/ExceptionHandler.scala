package ba.sake.sharaf.handlers

import scala.util.control.NonFatal
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.ExceptionMapper

final class ExceptionHandler(exceptionMapper: ExceptionMapper, wrappedHandler: SharafHandler) extends SharafHandler {

  override def handle(context: RequestContext): Response[?] =
    try wrappedHandler.handle(context)
    catch {
      case NonFatal(e) =>
        val errResponseOpt = exceptionMapper.lift(e)
        errResponseOpt match {
          case Some(response) =>
            response
          case None =>
            // if no error response match, just propagate.
            // will return 500
            throw e
        }
    }

}
