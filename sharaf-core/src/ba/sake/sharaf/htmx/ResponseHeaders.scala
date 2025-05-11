package ba.sake.sharaf.htmx

import ba.sake.sharaf.HttpString

object ResponseHeaders {

  /** allows you to do a client-side redirect that does not do a full page reload */
  val Location = HttpString("HX-Location")

  /** pushes a new url into the history stack */
  val PushUrl = HttpString("HX-Push-Url")

  /** can be used to do a client-side redirect to a new location */
  val Redirect = HttpString("HX-Redirect")

  /** if set to “true” the client-side will do a full refresh of the page */
  val Refresh = HttpString("HX-Refresh")

  /** replaces the current URL in the location bar */
  val ReplaceUrl = HttpString("HX-Replace-Url")

  /** allows you to specify how the response will be swapped. See hx-swap for possible values */
  val Reswap = HttpString("HX-Reswap")

  /** a CSS selector that updates the target of the content update to a different element on the page */
  val Retarget = HttpString("HX-Retarget")

  /** a CSS selector that allows you to choose which part of the response is used to be swapped in. Overrides an
    * existing hx-select on the triggering element
    */
  val Reselect = HttpString("HX-Reselect")

  /** allows you to trigger client-side events */
  val Trigger = HttpString("HX-Trigger")

  /** allows you to trigger client-side events after the settle step */
  val TriggerAfterSettle = HttpString("HX-Trigger-After-Settle")

  /** allows you to trigger client-side events after the swap step */
  val TriggerAfterSwap = HttpString("HX-Trigger-After-Swap")

}
