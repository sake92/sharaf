package utils

import ba.sake.hepek.bootstrap5.statik.BootstrapStaticBundle
import ba.sake.hepek.prismjs.PrismCodeHighlightComponents

val Bundle = locally {
  val b = BootstrapStaticBundle.default
  import b.*

  val ratios = Ratios.default.withSingle(1, 2, 1).withHalf(1, 1).withThird(1, 2, 1)
  val grid = Grid.withScreenRatios(
    Grid.screenRatios.withSm(None).withXs(None).withLg(ratios).withMd(ratios)
  )
  b.withGrid(grid)
}

val chl = PrismCodeHighlightComponents.default

val FA = ba.sake.hepek.fontawesome5.FA
