package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.core.config.Config
import org.pac4j.core.context.CallContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.testkit.FormScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils
import sttp.model.StatusCode

class FormSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.formConfig()
    val formClient = pac4jConfig.getClients.findClient("FormClient").get.asInstanceOf[FormClient]
    formClient.setAuthenticator(SharafTestAuthenticators.usernamePassword)
    val secConfig = Pac4jSecurityConfig(pac4jConfig, clients = "FormClient")

    // Protected route (secured by Pac4jSecurityHandler)
    val protectedHandler = Pac4jSecurityHandler(secConfig, SharafHandler.routes(Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }))

    // Login handler (unsecured - handles GET and POST /login)
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

    // Combined handler: /login → loginHandler, else → protectedHandler
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

  test("happy path") { FormScenarios.runHappyPath(serverUrl) }
  test("bad credentials") { FormScenarios.runBadCredentials(serverUrl) }
  test("redirect when unauthenticated") { FormScenarios.runRedirectWhenUnauthenticated(serverUrl) }

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
        val profileManagerFactory = pac4jConfig.getProfileManagerFactory
        if profileManagerFactory != null then
          val profileManager = profileManagerFactory.apply(webContext, sessionStore)
          profileManager.save(true, profile, false)
        Response.redirect("/protected").withStatus(StatusCode.Found)
    catch
      case e: Exception =>
        e.printStackTrace()
        Response.redirect("/login?error").withStatus(StatusCode.Found)
