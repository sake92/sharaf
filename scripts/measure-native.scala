//> using scala 3.9.0
//> using jvm temurin:21.0.10
//> using dep com.lihaoyi::mainargs:0.7.7
//> using dep ba.sake::tupson:0.31.0
//> using dep com.softwaremill.sttp.client4::core:4.0.27

import java.nio.file.{Files, Path}
import java.security.MessageDigest
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import mainargs.{main, arg, ParserForMethods}
import sttp.client4.quick.*
import ba.sake.tupson.*

object MeasureNative:
  private case class Greeting(message: String) derives JsonRW
  private case class Problem(status: Int, title: String) derives JsonRW
  private case class Sample(startupMs: Double, idleRssKiB: Long, afterRequestsRssKiB: Long) derives JsonRW
  private case class Result(
      variant: String,
      artifactBytes: Long,
      buildSeconds: Double,
      buildMaxRssKiB: Long,
      buildCommand: Seq[String],
      runCommand: Seq[String],
      samples: Seq[Sample]
  ) derives JsonRW
  private case class Variant(name: String, source: String, options: Seq[String], jvm: Boolean)

  private val examples = Path.of("examples/scala-cli")
  private val javaHome = Path.of(sys.props("java.home"))
  private val variants = Seq(
    Variant("jdk-jvm", "hello-jdk-graalvm.scala", Seq("--assembly", "--preamble=false"), true),
    Variant(
      "jdk-graalvm",
      "hello-jdk-graalvm.scala",
      Seq("--native-image", "--graalvm-jvm-id", "graalvm-community:25.0.2"),
      false
    ),
    Variant(
      "http4s-jvm",
      "hello-http4s-native.scala",
      Seq("--platform", "jvm", "--assembly", "--preamble=false"),
      true
    ),
    Variant(
      "http4s-native-fast",
      "hello-http4s-native.scala",
      Seq("--native-mode", "release-fast", "--native-multithreading"),
      false
    ),
    Variant(
      "http4s-native-size",
      "hello-http4s-native.scala",
      Seq("--native-mode", "release-size", "--native-multithreading"),
      false
    ),
    Variant("undertow-jvm", "hello-undertow.scala", Seq("--assembly", "--preamble=false"), true)
  )

  @main
  def measure(
      @arg(doc = "New output directory; existing paths are refused") output: String,
      @arg(doc = "Fresh process repetitions per variant") runs: Int = 5,
      @arg(doc = "Sequential mixed requests per repetition, after smoke checks") requests: Int = 300,
      @arg(doc = "Variant to build; repeat this option to select several; default is all") only: Seq[String] = Seq.empty
  ): Unit =
    require(System.getProperty("os.name") == "Linux", "RSS measurement requires Linux /proc")
    require(runs > 0 && requests > 0, "--runs and --requests must be positive")
    require(Files.isDirectory(examples), "Run from the Sharaf repository root")
    val selected = if only.isEmpty then variants else variants.filter(v => only.contains(v.name))
    require(
      only.forall(name => variants.exists(_.name == name)),
      s"Valid variants: ${variants.map(_.name).mkString(", ")}"
    )
    val out = Path.of(output).toAbsolutePath
    require(!Files.exists(out), s"Output already exists: $out; choose a new --output directory")
    checkPort()
    // Initialize the measurement client's backend before starting a timed server process.
    try response("/plaintext")
    catch case NonFatal(_) => ()
    Files.createDirectories(out)
    write(
      out.resolve("methodology.txt"),
      s"Process-cold startup on warm host/filesystem; HTTP polling interval 5ms, per-request timeout 500ms.\n" +
        s"Idle RSS: 1s after readiness; post-workload RSS: 1s after $requests sequential mixed requests.\n" +
        s"Build time includes Scala CLI startup/compilation/packaging and any downloads; caches are retained.\n" +
        s"Build max RSS is GNU time's maximum resident set size, not aggregate concurrent process-tree RSS.\n" +
        s"Artifact bytes exclude OS shared libraries; JVM artifacts additionally require the recorded JDK.\n" +
        s"JDK directory bytes are the installed full JDK, not a minimal jlink runtime.\n" +
        s"No throughput, machine-cold, container-size, TLS, or Lambda cold-start claims.\n"
    )
    val metadata = Map(
      "timestamp" -> java.time.Instant.now().toString,
      "revision" -> capture(Seq("git", "rev-parse", "HEAD"), out.resolve("revision.log")),
      "workingTree" -> capture(Seq("git", "status", "--short"), out.resolve("status.log")),
      "os" -> capture(Seq("uname", "-a"), out.resolve("os.log")),
      "cpu" -> Files
        .readString(Path.of("/proc/cpuinfo"))
        .linesIterator
        .find(_.startsWith("model name"))
        .getOrElse("unknown"),
      "java" -> capture(Seq(javaHome.resolve("bin/java").toString, "-version"), out.resolve("java.log")),
      "javaHome" -> javaHome.toString,
      "installedJdkBytes" -> directoryBytes(javaHome).toString,
      "scalaCli" -> capture(Seq("scala-cli", "version"), out.resolve("scala.log")),
      "clang" -> capture(Seq("clang", "--version"), out.resolve("clang.log")),
      "runs" -> runs.toString,
      "requests" -> requests.toString
    )
    write(out.resolve("metadata.json"), metadata.toJson)
    capture(Seq("git", "diff", "--binary", "HEAD"), out.resolve("source.diff"))
    val sources = variants.map(v => examples.resolve(v.source)).distinct :+
      examples.resolve("native/ExampleRoutes.scala") :+ Path.of("scripts/measure-native.scala")
    val hashes = sources.map { source =>
      val bytes = Files.readAllBytes(source)
      val target = out.resolve("sources").resolve(source)
      Files.createDirectories(target.getParent)
      Files.write(target, bytes)
      source.toString -> MessageDigest.getInstance("SHA-256").digest(bytes).map(b => f"${b & 0xff}%02x").mkString
    }.toMap
    write(out.resolve("source-sha256.json"), hashes.toJson)
    val results = selected.map { variant =>
      System.err.println(s"Building ${variant.name}...")
      val dir = Files.createDirectory(out.resolve(variant.name))
      val artifact = dir.resolve(if variant.jvm then "app.jar" else "app")
      val graalOptions =
        if variant.name == "jdk-graalvm" then Seq("--", "--no-fallback", "-march=compatibility") else Seq.empty
      val command = Seq(
        "scala-cli",
        "--power",
        "package",
        examples.resolve(variant.source).toString,
        "--server=false",
        "--jvm",
        "temurin:21.0.10",
        "-o",
        artifact.toString
      ) ++ variant.options ++ graalOptions
      write(dir.resolve("build-command.json"), command.toJson)
      val timed = Seq("/usr/bin/time", "-f", "%e\n%M", "-o", dir.resolve("build-time.txt").toString) ++ command
      withProcess(timed, dir.resolve("build.log")) { process =>
        require(process.waitFor(30, TimeUnit.MINUTES), s"Build timed out; see $dir/build.log")
        require(process.exitValue() == 0, s"Build failed; see $dir/build.log")
      }
      if !variant.jvm then capture(Seq("ldd", artifact.toString), dir.resolve("dynamic-libraries.txt"))
      val runCommand =
        if variant.jvm then Seq(javaHome.resolve("bin/java").toString, "-jar", artifact.toString)
        else Seq(artifact.toString)
      val samples = (1 to runs).map { index =>
        System.err.println(s"Checking ${variant.name}, repetition $index/$runs...")
        checkPort()
        val started = System.nanoTime()
        withProcess(runCommand, dir.resolve(s"run-$index.log")) { process =>
          awaitReady(process, 30.seconds)
          val startupMs = (System.nanoTime() - started) / 1e6
          Thread.sleep(1000)
          val idle = rss(process.pid())
          smoke()
          for i <- 0 until requests do
            i % 3 match
              case 0 => plain()
              case 1 => greeting("/hello/Scala", None, "Hello Scala")
              case _ => greeting("/hello", Some("{\"name\":\"Šaraf\"}"), "Hello Šaraf")
          Thread.sleep(1000)
          Sample(startupMs, idle, rss(process.pid()))
        }
      }
      val timing = Files.readAllLines(dir.resolve("build-time.txt")).asScala
      val result =
        Result(variant.name, Files.size(artifact), timing(0).toDouble, timing(1).toLong, command, runCommand, samples)
      write(dir.resolve("result.json"), result.toJson)
      result
    }
    write(out.resolve("results.json"), results.toJson)
    val rows = results.map { r =>
      val startup = median(r.samples.map(_.startupMs))
      val idle = median(r.samples.map(_.idleRssKiB.toDouble)) / 1024
      val after = median(r.samples.map(_.afterRequestsRssKiB.toDouble)) / 1024
      f"| ${r.variant} | ${r.artifactBytes / 1048576.0}%.2f | $startup%.1f | $idle%.1f | $after%.1f | ${r.buildSeconds}%.1f | ${r.buildMaxRssKiB / 1024.0}%.1f |"
    }
    write(
      out.resolve("results.md"),
      "# Local native deployment measurements\n\n" +
        "Medians across fresh processes. See `methodology.txt`, `metadata.json`, `results.json`, and build logs.\n\n" +
        "| Variant | Artifact MiB | First HTTP ms | Idle RSS MiB | After requests RSS MiB | Build seconds | Build max RSS MiB |\n" +
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: |\n" + rows.mkString("\n") + "\n"
    )
    System.err.println(s"Measurements and logs: $out")

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toSeq)

  private def response(path: String, body: Option[String] = None): sttp.client4.Response[String] =
    val base = "http://127.0.0.1:8181"
    val url = base + path
    val request = quickRequest.get(uri"$url").readTimeout(500.millis)
    body match
      case Some(json) => request.post(uri"$url").contentType("application/json").body(json).send()
      case None       => request.send()

  private def plain(): Unit =
    val res = response("/plaintext")
    require(res.code.code == 200 && res.body == "Hello Scala", s"Unexpected plain-text response: $res")

  private def greeting(path: String, body: Option[String], expected: String): Unit =
    val res = response(path, body)
    require(
      res.code.code == 200 && res.contentType.exists(_.startsWith("application/json")),
      s"Unexpected JSON response: $res"
    )
    require(res.body.parseJson[Greeting] == Greeting(expected), s"Unexpected greeting: ${res.body}")

  private def smoke(): Unit =
    plain()
    greeting("/hello/Scala", None, "Hello Scala")
    greeting("/hello", Some("{\"name\":\"Šaraf\"}"), "Hello Šaraf")
    for body <- Seq("{}", "{") do
      val res = response("/hello", Some(body))
      require(res.code.code == 400, s"Expected 400: $res")
      require(res.contentType.exists(_.startsWith("application/json")), s"Expected JSON problem content type: $res")
      require(res.body.parseJson[Problem].status == 400, s"Expected JSON problem: ${res.body}")
    require(response("/missing").code.code == 404, "Expected 404 for missing route")

  private def awaitReady(process: Process, timeout: FiniteDuration): Unit =
    val deadline = timeout.fromNow
    var ready = false
    var lastFailure = "No response"
    while !ready && deadline.hasTimeLeft() do
      require(process.isAlive, "Server exited before readiness; see run log")
      try
        plain()
        ready = true
      catch
        case NonFatal(error) =>
          lastFailure = error.toString
          Thread.sleep(5)
    require(ready, s"Server readiness timed out: $lastFailure; see run log")

  private def checkPort(): Unit =
    val socket = new ServerSocket()
    try
      socket.setReuseAddress(true)
      socket.bind(new java.net.InetSocketAddress("127.0.0.1", 8181))
    finally socket.close()

  private def rss(pid: Long): Long =
    val line = Files.readString(Path.of(s"/proc/$pid/status")).linesIterator.find(_.startsWith("VmRSS:")).get
    line.split("\\s+")(1).toLong

  private def withProcess[A](command: Seq[String], log: Path)(f: Process => A): A =
    val builder = new ProcessBuilder(command.asJava).redirectErrorStream(true).redirectOutput(log.toFile)
    val environment = builder.environment()
    val retained = Seq("PATH", "HOME", "JAVA_HOME", "COURSIER_CACHE", "SCALA_CLI_HOME", "TMPDIR", "LANG", "LC_ALL")
      .flatMap(key => Option(environment.get(key)).map(key -> _))
      .toMap
    environment.clear()
    retained.foreach((key, value) => environment.put(key, value))
    val process = builder.start()
    process.getOutputStream.close()
    val hook = new Thread(() => stop(process))
    Runtime.getRuntime.addShutdownHook(hook)
    try f(process)
    finally
      try stop(process)
      finally Runtime.getRuntime.removeShutdownHook(hook)

  private def stop(process: Process): Unit =
    val stream = process.descendants()
    val descendants =
      try stream.toList.asScala.toSeq
      finally stream.close()
    val handles = descendants :+ process.toHandle
    var interrupted = Thread.interrupted()
    def pause(): Unit =
      try Thread.sleep(20)
      catch case _: InterruptedException => interrupted = true
    try
      handles.filter(_.isAlive).foreach(_.destroy())
      val graceful = 3.seconds.fromNow
      while handles.exists(_.isAlive) && graceful.hasTimeLeft() do pause()
      handles.filter(_.isAlive).foreach(_.destroyForcibly())
      val forced = 3.seconds.fromNow
      while handles.exists(_.isAlive) && forced.hasTimeLeft() do pause()
      require(!handles.exists(_.isAlive), s"Process cleanup failed for PID ${process.pid()}")
      var reaped = false
      while !reaped && forced.hasTimeLeft() do
        try reaped = process.waitFor(20, TimeUnit.MILLISECONDS)
        catch case _: InterruptedException => interrupted = true
      require(reaped, s"Could not reap PID ${process.pid()}")
    finally if interrupted then Thread.currentThread().interrupt()

  private def capture(command: Seq[String], log: Path): String =
    withProcess(command, log) { process =>
      require(process.waitFor(30, TimeUnit.SECONDS), s"Command timed out: ${command.mkString(" ")}")
      require(process.exitValue() == 0, s"Command failed; see $log")
    }
    Files.readString(log).trim

  private def directoryBytes(root: Path): Long =
    val stream = Files.walk(root)
    try stream.iterator().asScala.filter(Files.isRegularFile(_)).map(Files.size).sum
    finally stream.close()

  private def write(path: Path, value: String): Unit = Files.writeString(path, value): Unit

  private def median(values: Seq[Double]): Double =
    val sorted = values.sorted
    val middle = sorted.size / 2
    if sorted.size % 2 == 0 then (sorted(middle - 1) + sorted(middle)) / 2 else sorted(middle)
