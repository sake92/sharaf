//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0
import java.util.concurrent.TimeUnit

// https://htmx.org/examples/progress-bar/
// scala htmx_progress_bar.sc --resource-dir resources

import java.util.concurrent.Executors
import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.sharaf.htmx.ResponseHeaders

var percentage = 0

val executor = Executors.newScheduledThreadPool(1)
var progressJob: java.util.concurrent.Future[?] = null

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case POST -> Path("start") =>
    percentage = 0
    progressJob = executor.scheduleAtFixedRate(
      { () =>
        percentage += scala.util.Random.nextInt(30) + 1
        if percentage >= 100 then progressJob.cancel(true)
      },
      0,
      1,
      TimeUnit.SECONDS
    )
    Response.withBody(progressBarWrapper(percentage))
  case GET -> Path("job") =>
    Response.withBody(progressBarWrapper(percentage))
  case GET -> Path("job", "progress") =>
    val bar = progressBar(percentage)
    if percentage >= 100
    then Response.withBody(bar).settingHeader(ResponseHeaders.Trigger, "done")
    else Response.withBody(bar)

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView = doctype("html")(
  html(
    head(
      tag("style")("""
        .progress {
            height: 20px;
            margin-bottom: 20px;
            overflow: hidden;
            background-color: #f5f5f5;
            border-radius: 4px;
            box-shadow: inset 0 1px 2px rgba(0,0,0,.1);
        }
        .progress-bar {
            float: left;
            width: 0%;
            height: 100%;
            font-size: 12px;
            line-height: 20px;
            color: #fff;
            text-align: center;
            background-color: #337ab7;
            -webkit-box-shadow: inset 0 -1px 0 rgba(0,0,0,.15);
            box-shadow: inset 0 -1px 0 rgba(0,0,0,.15);
            -webkit-transition: width .6s ease;
            -o-transition: width .6s ease;
            transition: width .6s ease;
        }
      """),
      script(src := "https://unpkg.com/htmx.org@2.0.4")
    ),
    body(
      div(hx.target := "this", hx.swap := "outerHTML")(
        h3("Start Progress"),
        button(hx.post := "/start")("Start Job")
      )
    )
  )
)

def progressBarWrapper(currentPercentage: Int) =
  val completed = currentPercentage >= 100
  div(hx.get := "/job", hx.trigger := "done", hx.target := "this", hx.swap := "outerHTML")(
    h3(role := "status", id := "pblabel", tabindex := "-1")(if completed then "Completed" else "Running"),
    progressBar(currentPercentage),
    Option.when(completed)(
      button(hx.post := "/start")("Restart Job")
    )
  )

def progressBar(currentPercentage: Int) =
  val completed = currentPercentage >= 100
  div(
    hx.get := "/job/progress",
    Option.unless(completed)(hx.trigger := "every 600ms"),
    hx.target := "this",
    hx.swap := "innerHTML"
  )(
    div(
      cls := "progress",
      role := "progressbar",
      aria.valuemin := "0",
      aria.valuemax := "100",
      aria.valuenow := currentPercentage,
      aria.labelledby := "pblabel"
    )(
      div(id := "pb", cls := "progress-bar", style := s"width:${currentPercentage}%")
    )
  )
