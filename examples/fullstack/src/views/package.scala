package fullstack.views

import ba.sake.hepek.bootstrap3.BootstrapBundle

val Bundle = locally {
  val b = BootstrapBundle.default
  b.withGrid(
    b.Grid.withScreenRatios(
      b.Grid.screenRatios
        .withLg(b.Ratios.default.withSingle(1, 4, 1))
        .withMd(b.Ratios.default.withSingle(1, 4, 1))
        .withSm(None) // stack on small
        .withXs(None) // and extra-small screens
    )
  )
}

trait MyPage extends Bundle.Page
