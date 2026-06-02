package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.core.config.Config
import org.pac4j.core.context.CallContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.testkit.CookieScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils
import sttp.model.StatusCode

class CookieSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.cookieConfig()
    val formClient = pac4jConfig.getClients.findClient("FormClient").get.asInstanceOf[FormClient]
    formClient.setAuthenticator(SharafTestAuthenticators.usernamePassword)
    // In pac4j 6.5.2, with both HeaderClient and FormClient, the direct HeaderClient returns 401
    // before the indirect FormClient can redirect. Keep only FormClient for the cookie test.
    import scala.jdk.CollectionConverters.*
    val formOnly = pac4jConfig.getClients.findAllClients.asScala.filter(_.getName == "FormClient").asJava
    pac4jConfig.getClients.setClients(formOnly)
    val secConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "FormClient",
      logoutPath = Some("/logout"),
      defaultLogoutUrl = "/",
    )

    // Protected routes (secured - also handles /logout internally)
    val protectedHandler = Pac4jSecurityHandler(secConfig, SharafHandler.routes(Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }))

    // Login handler (unsecured)
    val loginHandler = new SharafHandler {
      override def handle(ctx: RequestContext): Response[?] =
        ctx.params match
          case (HttpMethod.POST, Path("login")) =>
            handleFormLoginPost(ctx.request, pac4jConfig)
          case _ =>
            Response.withBody(
              "<html><body>" +
              "<form action='/login' method='post'>" +
              "<input name='username'/><input name='password'/>" +
              "<input type='submit'/>" +
              "</form></body></html>"
            )
    }

    // Combined handler
    val combinedHandler = new SharafHandler {
      override def handle(ctx: RequestContext): Response[?] =
        val pathStr = "/" + ctx.params._2.segments.mkString("/")
        if pathStr.startsWith("/login") then loginHandler.handle(ctx)
        else protectedHandler.handle(ctx)
    }

    // Wrap with session handler so cookies are loaded/saved across requests
    val handler = SharafHandler.sessions(combinedHandler)

    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("happy path") { CookieScenarios.runHappyPath(serverUrl) }
  test("logout") { CookieScenarios.runLogout(serverUrl) }

  private def handleFormLoginPost(req: Request, pac4jConfig: Config): Response[?] =
    import ba.sake.formson.FormValue
    val formParams = req.bodyFormRaw
    val username = formParams.getOrElse("username", Seq.empty).collectFirst { case FormValue.Str(s) => s }.getOrElse("")
    val password = formParams.getOrElse("password", Seq.empty).collectFirst { case FormValue.Str(s) => s }.getOrElse("")

    val formClientOpt = pac4jConfig.getClients.findClient("FormClient")
    if formClientOpt.isEmpty then
      return Response.withStatus(StatusCode.InternalServerError)
    val formClient = formClientOpt.get.asInstanceOf[FormClient]

    val credentials = UsernamePasswordCredentials(username, password)
    val portPart = if serverUrl.contains(":") then serverUrl.split(":")(2) else "80"
    val fullUrl = s"http://localhost:$portPart/login"
    val webContext = SharafWebContext(req, fullUrl, HttpMethod.POST)
    val sessionStore = new SharafSessionStore(ba.sake.sharaf.session.InMemorySessionStore())
    val callContext = CallContext(webContext, sessionStore)

    try
      val validatedOpt = formClient.getAuthenticator.validate(callContext, credentials)
      if validatedOpt.isEmpty || validatedOpt.get.getUserProfile == null then
        Response.redirect("/login?error").withStatus(StatusCode.Found)
      else
        val profile = validatedOpt.get.getUserProfile
        pac4jConfig.getProfileManagerFactory
          .apply(webContext, sessionStore)
          .save(true, profile, false)
        Response.redirect("/protected").withStatus(StatusCode.Found)
    catch
      case e: Exception =>
        e.printStackTrace()
        Response.redirect("/login?error").withStatus(StatusCode.Found)
