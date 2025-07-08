//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1
import java.util.concurrent.TimeUnit

// https://htmx.org/examples/progress-bar/

import java.util.concurrent.Executors
import ba.sake.sharaf.{*, given}
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

def IndexView =
  html"""
    <!DOCTYPE html>
    <html>
    <head>
      <script src="https://unpkg.com/htmx.org@2.0.4"></script>
      <style>
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
        </style>
    </head>
    <body>
        <div hx-target="this" hx-swap="outerHTML">
            <h3>Start Progress</h3>
            <button hx-post="/start">Start Job</button>
        </div>
    </body>
    </html>
  """

def progressBarWrapper(currentPercentage: Int) =
  val completed = currentPercentage >= 100
  html"""
  <div hx-get="/job" hx-trigger="done" hx-target="this" hx-swap="outerHTML">
        <h3 role="status" id="pblabel" tabindex="-1">
        ${if completed then "Completed" else "Running"}
        </h3>
        ${progressBar(currentPercentage)}
        ${Option.when(completed)(
      html""" <button hx-post="/start">Restart Job</button> """
    )}
  </div>
  """

def progressBar(currentPercentage: Int) =
  val completed = currentPercentage >= 100
  html"""
    <div hx-get="/job/progress"
       ${Option.unless(completed)(html""" hx-trigger="every 600ms" """)}
       hx-target="this"
       hx-swap="innerHTML">
        <div class="progress"
            role="progressbar"
            aria-valuemin="0"
            aria-valuemax="100"
            aria-valuenow="${currentPercentage}"
            aria-labelledby="pblabel">
        <div id="pb" class="progress-bar" style="width:${currentPercentage}%"></div>
        </div>
    </div>
  """
