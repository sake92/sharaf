package demo

import java.{util => ju}

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import org.pac4j.core.client.Client
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.DefaultSecurityLogic
import org.pac4j.core.exception.http.HttpAction
import org.pac4j.core.exception.http.UnauthorizedAction

class CustomSecurityLogic extends DefaultSecurityLogic {

  override protected def redirectToIdentityProvider(
      context: WebContext,
      sessionStore: SessionStore,
      currentClients: ju.List[Client]
  ): HttpAction = {
    // Pac4J redirects to the FIRST CLIENT by default
    // here we take the desired login method from the *query parameter*
    // https://stackoverflow.com/questions/68428308/in-which-order-are-pac4j-client-used
    val providerOpt = context.getRequestParameter("provider").toScala
    providerOpt match
      case None =>
        // we return 401 if not authenticated
        // you *could* set a default client to be redirected to
        return UnauthorizedAction()
      case Some(clientName) =>
        currentClients.asScala.find(_.getName() == clientName) match
          case None =>
            val action = UnauthorizedAction()
            action.setContent("Unsupported provider")
            action
          case Some(client) => client.getRedirectionAction(context, sessionStore).get()

  }
}
