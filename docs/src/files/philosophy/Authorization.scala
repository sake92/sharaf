package files.philosophy


import utils.*

object Authorization extends PhilosophyPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Authorization")
    .withLabel("Authorization")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to implement authorization?",
    s"""
    This is a complex topic, and there are many ways to do it.  
    Some general guidelines we should follow are defined in the [OWASP authz cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html).  
    An important point is to "Prefer Attribute and Relationship Based Access Control over RBAC".
    
    
  1. 
    ```scala
    def AuthenticatedRoutes(handler: User ?=> RoutesDefinition): Routes =
  Routes:
    Request.current.headers.get(HttpString("Authorization")) match
      case Authenticated(user) =>
        given User = user
        handler
      case _ =>
        // not used, provided only to access the partial function
        given User = User("fake")
        {
          case t if handler.isDefinedAt(t) =>
            Response.withStatus(401).withBody("Unauthorized")
        }
    ```

    
    """.md
  )
}
