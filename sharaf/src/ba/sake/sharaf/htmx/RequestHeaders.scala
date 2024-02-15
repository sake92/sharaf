package ba.sake.sharaf.htmx

import io.undertow.util.HttpString

object RequestHeaders {

  /** indicates that the request is via an element using hx-boost */
  val Boosted = HttpString("HX-Boosted")

  /** the current URL of the browser */
  val CurrentURL = HttpString("HX-Current-URL")

  /** "true" if the request is for history restoration after a miss in the local history cache */
  val HistoryRestoreRequest = HttpString("HX-History-Restore-Request")

  /** the user response to an hx-prompt */
  val Prompt = HttpString("HX-Prompt")

  /** always "true" */
  val Request = HttpString("HX-Request")

  /** the id of the target element if it exists */
  val Target = HttpString("HX-Target")

  /** the name of the triggered element if it exists */
  val TriggerName = HttpString("HX-Trigger-Name")

  /** the id of the triggered element if it exists */
  val Trigger = HttpString("HX-Trigger")

}
