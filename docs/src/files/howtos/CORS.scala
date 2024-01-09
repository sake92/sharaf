package files.howtos

import utils.Bundle.*

object CORS extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Set CORS")
    .withLabel("CORS")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to set up CORS?",
    s"""
    By default, Sharaf sets no permitted origins.  
    This means you can only use the API/website from the same domain.

    If you want to configure it to be available for other domains, 
    use the `withCorsSettings` method and set desired config:
    ```scala
    import ba.sake.sharaf.handlers.cors.CorsSettings
    import ba.sake.sharaf.*, routing.*

    SharafHandler(routes).withCorsSettings(
      CorsSettings.default.withAllowedOrigins(Set("https://example.com"))
    )
    ```
    """.md
  )
}
