package ba.sake.sharaf.pac4j

import ba.sake.sharaf.*

extension (handlerObj: SharafHandler.type)

  def pac4j(
      routes: SharafHandler,
      securityConfig: Pac4jSecurityConfig,
  ): SharafHandler = {
    val securityHandler = Pac4jSecurityHandler(securityConfig, routes)

    val hasCallback = securityConfig.callbackPath.isDefined
    val hasLogout = securityConfig.logoutPath.isDefined

    if !hasCallback && !hasLogout then securityHandler
    else {
      val callbackSegments = securityConfig.callbackPath.toSeq
        .flatMap(_.split("/")).filter(_.nonEmpty)
      val logoutSegments = securityConfig.logoutPath.toSeq
        .flatMap(_.split("/")).filter(_.nonEmpty)

      new SharafHandler {
        private val callbackHandler: Option[SharafHandler] =
          if hasCallback then
            Some(Pac4jCallbackHandler(securityConfig.pac4jConfig))
          else None

        private val logoutHandler: Option[SharafHandler] =
          if hasLogout then
            Some(Pac4jLogoutHandler(securityConfig.pac4jConfig, securityConfig.defaultLogoutUrl))
          else None

        override def handle(ctx: RequestContext): Response[?] = {
          val pathSegments = ctx.params._2.segments
          if hasCallback && pathSegments == callbackSegments then
            callbackHandler.get.handle(ctx)
          else if hasLogout && pathSegments == logoutSegments then
            logoutHandler.get.handle(ctx)
          else
            securityHandler.handle(ctx)
        }
      }
    }
  }
