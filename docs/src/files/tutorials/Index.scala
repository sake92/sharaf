package files.tutorials

import utils.*

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
        Create a file `my_script.sc` with the following content:
        ```scala
        //> using dep ${Consts.ArtifactOrg}::${Consts.ArtifactName}:${Consts.ArtifactVersion}
        ```
        and then run it with:
        ```bash
        scala-cli my_script.sc --scala-option -Yretain-trees
        ```
        """.md
      ),
      Section(
        "Examples",
        s"""
        - [scala-cli examples](${Consts.GhSourcesUrl}/examples/scala-cli), standalone examples using scala-cli
        - [scala-cli HTMX examples](${Consts.GhSourcesUrl}/examples/htmx), standalone examples featuring HTMX
        - [API example](${Consts.GhSourcesUrl}/examples/api) featuring JSON and validation
        - [full-stack example](${Consts.GhSourcesUrl}/examples/fullstack) featuring HTML, static files and forms
        - [sharaf-todo-backend](https://github.com/sake92/sharaf-todo-backend), implementation of the [todobackend.com](http://todobackend.com/) spec, featuring CORS handling
        - [OAuth2 login](${Consts.GhSourcesUrl}/examples/oauth2) with [Pac4J library](https://www.pac4j.org/)
        - [PetClinic](https://github.com/sake92/sharaf-petclinic) implementation, featuring full-stack app with Postgres db, config, integration tests etc.
        - [Giter8 template for fullstack app](https://github.com/sake92/sharaf-fullstack.g8)

        """.md
      )
    )
  )
}
