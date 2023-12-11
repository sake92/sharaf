package utils

import ba.sake.hepek.html.ComponentSettings
import ba.sake.hepek.theme.bootstrap5.*
import Bundle.*

trait SharafDocPage extends StaticPage with HepekBootstrap5BlogPage {

  override def staticSiteSettings = super.staticSiteSettings
    .withIndexPage(files.Index)
    .withMainPages(files.Index)

  override def siteSettings = super.siteSettings
    .withName("myblog.com")
    .withFaviconNormal(files.images.`favicon.ico`.ref)
    .withFaviconInverted(files.images.`favicon.ico`.ref)

  override def tocSettings = Some(TocSettings(tocType = TocType.Scrollspy(offset = 60)))

  override def bootstrapSettings = super.bootstrapSettings.withDepsProvider(DependencyProvider.unpkg)

  override def bootstrapDependencies = super.bootstrapDependencies
    .withCssDependencies(
      Dependencies.default.withDeps(
        Dependency("dist/flatly/bootstrap.min.css", bootstrapSettings.version, "bootswatch")
      )
    )

  override def styleURLs = super.styleURLs
    .appended(files.styles.`main.css`.ref)

  override def scriptURLs = super.scriptURLs
    .appended(files.scripts.`main.js`.ref)
}
