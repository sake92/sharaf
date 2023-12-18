package utils

import ba.sake.hepek.html.ComponentSettings
import ba.sake.hepek.prismjs.PrismDependencies
import ba.sake.hepek.theme.bootstrap5.*
import ba.sake.hepek.anchorjs.AnchorjsDependencies
import ba.sake.hepek.fontawesome5.FADependencies
import Bundle.*, Tags.*

trait DocStaticPage extends StaticPage with AnchorjsDependencies with FADependencies {
  override def staticSiteSettings = super.staticSiteSettings
    .withIndexPage(files.Index)
    .withMainPages(
      files.tutorials.Index,
      files.howtos.Index,
      files.reference.Index,
      files.philosophy.Index
    )

  override def siteSettings = super.siteSettings
    .withName(Consts.ProjectName)
    .withFaviconNormal(files.images.`favicon.ico`.ref)
    .withFaviconInverted(files.images.`favicon.ico`.ref)

  override def bodyContent = frag(
    super.bodyContent,
    footer(Classes.txtAlignCenter, Classes.bgInfo, cls := "fixed-bottom")(
      a(href := Consts.GhUrl, Classes.btnClass)(FA.github())
    )
  )

  override def styleURLs = super.styleURLs
    .appended(files.styles.`main.css`.ref)

  override def scriptURLs = super.scriptURLs
    .appended(files.scripts.`main.js`.ref)

  // you can set a custom Bootstrap theme..
  /*
  override def bootstrapSettings = super.bootstrapSettings.withDepsProvider(DependencyProvider.unpkg)

  override def bootstrapDependencies = super.bootstrapDependencies
    .withCssDependencies(
      Dependencies.default.withDeps(
        Dependency("dist/flatly/bootstrap.min.css", bootstrapSettings.version, "bootswatch")
      )
    )
   */

}

trait DocPage extends DocStaticPage with HepekBootstrap5BlogPage with PrismDependencies {

  override def tocSettings = Some(TocSettings(tocType = TocType.Scrollspy(offset = 60)))

  override def pageHeader = None

}
