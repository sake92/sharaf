package files.tutorials

import utils.*
import Bundle.*, Tags.*

object Index extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Tutorials")
    .withLabel("Tutorials")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Quickstart",
    s"""
      Hello world!
    """.md,
    List(
      Section(
        "Mill",
        s"""
        ```scala
        def ivyDeps = super.ivyDeps() ++ Agg(
          ivy"${Consts.ArtifactOrg}::${Consts.ArtifactName}:${Consts.ArtifactVersion}"
        )
        def scalacOptions = super.scalacOptions() ++ Seq("-Yretain-trees")
        ```
        """.md
      ),
      Section(
        "Sbt",
        s"""
        ```scala
        libraryDependencies ++= Seq(
          "${Consts.ArtifactOrg}" %% "${Consts.ArtifactName}" % "${Consts.ArtifactVersion}"
        )
        scalacOptions ++= Seq("-Yretain-trees")
        ```
        """.md
      ),
      Section(
        "Scala CLI",
        s"""
        ```scala
        //> using dep ${Consts.ArtifactOrg}::${Consts.ArtifactName}:${Consts.ArtifactVersion}
        ```
        """.md
      ),
      Section(
        "Examples",
        s"""
        ..
        """.md
      )
    )
  )
}
