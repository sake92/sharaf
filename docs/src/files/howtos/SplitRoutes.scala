package files.howtos

import utils.Bundle.*

object SplitRoutes extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Split Routes")
    .withLabel("Split Routes")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to split Routes?",
    s"""

    When you have lots of routes, you will want to split them into multiple `Routes` handlers.  
    Combining them is done with `Routes.merge`.  
    The order of routes is preserved, of course:
    ```scala
    val routes: Seq[Routes] = Seq(routes1, routes2, ... )

    val allRoutes: Routes = Routes.merge(routes)
    ```

    """.md
  )
}
