package files.tutorials

import utils.*
import Bundle.*

object Index extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Tutorials")
    .withLabel("Tutorials")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Quickstart",
    s"""Get started quickly with Sharaf framework.""".md,
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

        There are Giter8 templates available:
        - [fullstack](https://github.com/sake92/sharaf-fullstack.g8)

        """.md
      ),
      Section(
        "Sbt",
        s"""
        ```scala
        libraryDependencies ++= Seq(
          "${Consts.ArtifactOrg}" %% "${Consts.ArtifactName}" % "${Consts.ArtifactVersion}"
        ),
        scalacOptions ++= Seq("-Yretain-trees")
        ```
        """.md
      ),
      Section(
        "Scala CLI",
        s"""
        ```scala
        //> using dep ${Consts.ArtifactOrg}::${Consts.ArtifactName}:${Consts.ArtifactVersion}
        scala-cli my_script.sc --scala-option -Yretain-trees
        ```
        """.md
      ),
      Section(
        "Examples",
        s"""
        - [scala-cli examples](${Consts.GhSourcesUrl}/examples/scala-cli), a bunch of standalone examples
        - [API example](${Consts.GhSourcesUrl}/examples/api) featuring JSON and validation
        - [full-stack example](${Consts.GhSourcesUrl}/examples/fullstack) featuring HTML, static files and forms
        - [sharaf-todo-backend](https://github.com/sake92/sharaf-todo-backend), implementation of the [todobackend.com](http://todobackend.com/) spec, featuring CORS handling
        - [OAuth2 login](${Consts.GhSourcesUrl}/examples/oauth2) with [Pac4J library](https://www.pac4j.org/)
        - [PetClinic](https://github.com/sake92/sharaf-petclinic) implementation, featuring full-stack app with Postgres db, config, integration tests etc.

        """.md
      )
    )
  )
}
