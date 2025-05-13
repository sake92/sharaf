package files.philosophy

import utils.Bundle.*
import utils.Consts

object Index extends PhilosophyPage {

  override def pageSettings =
    super.pageSettings.withTitle("Philosophy")

  override def blogSettings =
    super.blogSettings.withSections(firstSection, nameSection)

  val firstSection = Section(
    "Why Sharaf?",
    s"""
    Simplicity and ease of use is the main focus of Sharaf.  

    Sharaf is built on top of [Undertow](https://undertow.io/).  
    This means you can use awesome libraries built for Undertow, like [pac4j](https://github.com/pac4j/undertow-pac4j) for security and others.  
    You can leverage Undertow's lower level API, e.g. for WebSockets.

    Sharaf bundles a set of standalone libraries:
    - [querson](${Consts.GhSourcesUrl}/querson) for query parameters
    - [tupson](https://github.com/sake92/tupson) for JSON
    - [formson](${Consts.GhSourcesUrl}/formson) for forms
    - [validson](${Consts.GhSourcesUrl}/validson) for validation
    - [hepek-components](https://github.com/sake92/hepek) for HTML (with [scalatags](https://github.com/com-lihaoyi/scalatags))
    - [sttp](https://sttp.softwaremill.com/en/latest/) for firing HTTP requests
    - [typesafe-config](https://github.com/lightbend/config) for configuration

    You can use any of above separately in your projects.

    """.md
  )
  val nameSection = Section(
    """Why name "Sharaf"?""",
    s"""
    Šaraf means a "screw" in Bosnian, which reminds me of scala spiral logo.  
    It's a germanism I think.

    """.md
  )
}
