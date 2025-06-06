package utils

import scalatags.Text.all.*
import scalatags.Text.tags2.{aside, main, nav, section}
import ba.sake.hepek.anchorjs.AnchorjsDependencies
import ba.sake.hepek.html.statik.{BlogPostPage, ShikiSettings, StaticPage}
import files.SearchResults

trait DocPage extends DocStaticPage with BlogPostPage {

  override def mainContent: Frag = div(cls := "blog-post")(
    aside(id := "left-menu")(
      nav(
        ul(
          for sameCatPost <- categoryPosts
          yield li(
            a(
              href := sameCatPost.ref,
              Option.when(this.relPath == sameCatPost.relPath)(attr("aria-current") := "page")
            )(
              sameCatPost.pageSettings.label
            )
          )
        )
      )
    ),
    div(cls := "main-content")(
      pager(this),
      renderSections(blogSettings.sections, 2)
    )
  )

  private def renderSections(secs: List[Section], depth: Int): List[Frag] =
    secs.map { s =>
      // h2, h3...
      val hTag = tag("h" + depth)
      section(id := s.id)(
        hTag(s.name),
        s.content,
        renderSections(s.children, depth + 1)
      )
    }
}

trait DocStaticPage extends StaticPage with AnchorjsDependencies {

  def currentCategoryPage: Option[StaticPage] = None

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
    .withFaviconNormal(files.images.`favicon.png`.ref)

  override def shikiSettings = super.shikiSettings.withTheme("material-theme-ocean")

  override def styleURLs =
    List("https://cdn.jsdelivr.net/npm/@picocss/pico@2.1.1/css/pico.cyan.min.css", files.styles.`main.css`.ref)

  override def pageContent = frag(
    header(cls := "container")(
      topNavbar
    ),
    main(cls := "container")(
      mainContent
    ),
    footer(cls := "container flex-centered")(
      a(href := "https://github.com/sake92/sharaf")(
        raw("""
            <?xml version="1.0" encoding="utf-8"?><!-- Uploaded to: SVG Repo, www.svgrepo.com, Generator: SVG Repo Mixer Tools -->
            <svg width="24px" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M12 0C5.37 0 0 5.37 0 12C0 17.31 3.435 21.795 8.205 23.385C8.805 23.49 9.03 23.13 9.03 22.815C9.03 22.53 9.015 21.585 9.015 20.58C6 21.135 5.22 19.845 4.98 19.17C4.845 18.825 4.26 17.76 3.75 17.475C3.33 17.25 2.73 16.695 3.735 16.68C4.68 16.665 5.355 17.55 5.58 17.91C6.66 19.725 8.385 19.215 9.075 18.9C9.18 18.12 9.495 17.595 9.84 17.295C7.17 16.995 4.38 15.96 4.38 11.37C4.38 10.065 4.845 8.985 5.61 8.145C5.49 7.845 5.07 6.615 5.73 4.965C5.73 4.965 6.735 4.65 9.03 6.195C9.99 5.925 11.01 5.79 12.03 5.79C13.05 5.79 14.07 5.925 15.03 6.195C17.325 4.635 18.33 4.965 18.33 4.965C18.99 6.615 18.57 7.845 18.45 8.145C19.215 8.985 19.68 10.05 19.68 11.37C19.68 15.975 16.875 16.995 14.205 17.295C14.64 17.67 15.015 18.39 15.015 19.515C15.015 21.12 15 22.41 15 22.815C15 23.13 15.225 23.505 15.825 23.385C18.2072 22.5808 20.2773 21.0498 21.7438 19.0074C23.2103 16.9651 23.9994 14.5143 24 12C24 5.37 18.63 0 12 0Z" fill="#000000"/>
            </svg>
            """)
      ),
      a(href := "https://discord.gg/g9KVY3WkMG")(
        raw("""
            <?xml version="1.0" encoding="utf-8"?><!-- Uploaded to: SVG Repo, www.svgrepo.com, Generator: SVG Repo Mixer Tools -->
            <svg width="24px" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
               <circle cx="512" cy="512" r="512" style="fill:#5865f2"/>
               <path d="M689.43 349a422.21 422.21 0 0 0-104.22-32.32 1.58 1.58 0 0 0-1.68.79 294.11 294.11 0 0 0-13 26.66 389.78 389.78 0 0 0-117.05 0 269.75 269.75 0 0 0-13.18-26.66 1.64 1.64 0 0 0-1.68-.79A421 421 0 0 0 334.44 349a1.49 1.49 0 0 0-.69.59c-66.37 99.17-84.55 195.9-75.63 291.41a1.76 1.76 0 0 0 .67 1.2 424.58 424.58 0 0 0 127.85 64.63 1.66 1.66 0 0 0 1.8-.59 303.45 303.45 0 0 0 26.15-42.54 1.62 1.62 0 0 0-.89-2.25 279.6 279.6 0 0 1-39.94-19 1.64 1.64 0 0 1-.16-2.72c2.68-2 5.37-4.1 7.93-6.22a1.58 1.58 0 0 1 1.65-.22c83.79 38.26 174.51 38.26 257.31 0a1.58 1.58 0 0 1 1.68.2c2.56 2.11 5.25 4.23 8 6.24a1.64 1.64 0 0 1-.14 2.72 262.37 262.37 0 0 1-40 19 1.63 1.63 0 0 0-.87 2.28 340.72 340.72 0 0 0 26.13 42.52 1.62 1.62 0 0 0 1.8.61 423.17 423.17 0 0 0 128-64.63 1.64 1.64 0 0 0 .67-1.18c10.68-110.44-17.88-206.38-75.7-291.42a1.3 1.3 0 0 0-.63-.63zM427.09 582.85c-25.23 0-46-23.16-46-51.6s20.38-51.6 46-51.6c25.83 0 46.42 23.36 46 51.6.02 28.44-20.37 51.6-46 51.6zm170.13 0c-25.23 0-46-23.16-46-51.6s20.38-51.6 46-51.6c25.83 0 46.42 23.36 46 51.6.01 28.44-20.17 51.6-46 51.6z" style="fill:#fff"/>
            </svg>
            """)
      )
    )
  )

  private def topNavbar = nav(
    ul(
      siteSettings.faviconNormal.map { fav =>
        li(img(src := fav))
      },
      li(a(href := "https://sake92.github.io/sharaf/")("Sharaf Docs"))
    ),
    ul(
      li(
        form(action := SearchResults.ref, method := "GET")(
          input(
            name := "q",
            tpe := "search",
            placeholder := "Search"
          )
        )
      )
    ),
    ul(
      for
        mainPage <- staticSiteSettings.mainPages
        labela = mainPage.pageCategory.getOrElse(mainPage.pageSettings.label)
      yield li(
        a(
          href := mainPage.ref,
          Option.when(currentCategoryPage.map(_.relPath).contains(mainPage.relPath))(
            attr("aria-current") := "true"
          )
        )(labela)
      )
    )
  )

  override def scriptURLs = super.scriptURLs
    .appended(files.scripts.`main.js`.ref)

}
