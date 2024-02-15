package ba.sake.sharaf.htmx

import ba.sake.sharaf.Request
import ba.sake.sharaf.htmx.RequestHeaders as Hx

extension (req: Request) {

  /** @return
    *   true if it is an HTMX request
    */
  def isHtmx: Boolean =
    val headerValueOpt = req.headers.get(Hx.Request).flatMap(_.headOption)
    headerValueOpt == Some("true")

  /** @return
    *   true if it is via an element using hx-boost
    */
  def isHtmxBoosted: Boolean =
    val headerValueOpt = req.headers.get(Hx.Boosted).flatMap(_.headOption)
    headerValueOpt == Some("true")

  /** @return
    *   the current URL of the browser, or empty string if not HTMX request
    */
  def htmxCurrentURL: String =
    val headerValueOpt = req.headers.get(Hx.CurrentURL).flatMap(_.headOption)
    headerValueOpt.getOrElse("")

  /** @return
    *   true if the request is for history restoration after a miss in the local history cache
    */
  def isHtmxHistoryRestore: Boolean =
    val headerValueOpt = req.headers.get(Hx.HistoryRestoreRequest).flatMap(_.headOption)
    headerValueOpt == Some("true")

  /** @return
    *   the user response to an hx-prompt, or empty string
    */
  def htmxPrompt: String =
    val headerValueOpt = req.headers.get(Hx.Prompt).flatMap(_.headOption)
    headerValueOpt.getOrElse("")

  /** @return
    *   the id of the target element if it exists
    */
  def htmxTarget: Option[String] =
    req.headers.get(Hx.Target).flatMap(_.headOption)

  /** @return
    *   the name of the triggered element if it exists
    */
  def htmxTriggerName: Option[String] =
    req.headers.get(Hx.TriggerName).flatMap(_.headOption)

  /** @return
    *   the id of the triggered element if it exists
    */
  def htmxTriggerId: Option[String] =
    req.headers.get(Hx.Trigger).flatMap(_.headOption)
}