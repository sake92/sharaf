package demo.views

import ba.sake.hepek.bootstrap3.BootstrapBundle
import ba.sake.hepek.html.component.GridComponents.Ratios
import ba.sake.hepek.bootstrap3.component.Bootstrap3MarkdownComponents

val Bundle = locally {
  val bb = BootstrapBundle()
  bb.withGrid(
    bb.Grid.withScreenRatios(
      bb.Grid.screenRatios
        .withLg(Ratios().withSingle(1, 4, 1))
        .withMd(Ratios().withSingle(1, 4, 1))
        .withSm(None) // stack on small
        .withXs(None) // and extra-small screens
    )
  )
}

trait MyPage extends Bundle.HtmlPage with Bootstrap3MarkdownComponents
