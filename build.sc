import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import $ivy.`ba.sake::mill-hepek::0.0.2`

import mill._
import mill.scalalib._, scalafmt._, publish._
import coursier.maven.MavenRepository
import io.kipp.mill.ci.release.CiReleaseModule
import ba.sake.millhepek.MillHepekModule

object sharaf extends SharafPublishModule {

  def artifactName = "sharaf"

  def ivyDeps = Agg(
    ivy"io.undertow:undertow-core:2.3.10.Final",
    ivy"com.lihaoyi::requests:0.8.0",
    ivy"ba.sake::tupson:0.11.0",
    ivy"ba.sake::tupson-config:0.11.0",
    ivy"ba.sake::hepek-components:0.23.0"
  )

  def moduleDeps = Seq(querson, formson)

  object test extends ScalaTests with SharafTestModule
}

object querson extends SharafPublishModule {

  def artifactName = "querson"

  def moduleDeps = Seq(validson)

  def pomSettings = super.pomSettings().copy(description = "Simple query params library")

  def ivyDeps = Agg(
    ivy"com.lihaoyi::fastparse:3.0.1"
  )

  object test extends ScalaTests with SharafTestModule
}

object formson extends SharafPublishModule {

  def artifactName = "formson"

  def moduleDeps = Seq(validson)

  def pomSettings = super.pomSettings().copy(description = "Simple form binding library")

  object test extends ScalaTests with SharafTestModule

  def ivyDeps = Agg(
    ivy"com.lihaoyi::fastparse:3.0.1"
  )
}

object validson extends SharafPublishModule {

  def artifactName = "validson"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode::0.3.0"
  )

  def pomSettings = super.pomSettings().copy(description = "Simple validation library")

  object test extends ScalaTests with SharafTestModule
}

trait SharafPublishModule extends SharafCommonModule with CiReleaseModule {

  def pomSettings = PomSettings(
    organization = "ba.sake",
    url = "https://github.com/sake92/sharaf",
    licenses = Seq(License.Common.Apache2),
    versionControl = VersionControl.github("sake92", "sharaf"),
    description = "Sharaf http library",
    developers = Seq(
      Developer("sake92", "Sakib Hadžiavdić", "https://sake.ba")
    )
  )
}

trait SharafCommonModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.3.1"
  def scalacOptions = super.scalacOptions() ++ Seq(
    "-Yretain-trees", // needed for default parameters
    "-deprecation",
    "-Wunused:all",
    "-explain"
  )
  def repositoriesTask = T.task {
    super.repositoriesTask() ++
      Seq(MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"))

  }
}

trait SharafTestModule extends TestModule.Munit {
  def ivyDeps = Agg(
    ivy"org.scalameta::munit::0.7.29"
  )
}

//////////////////// examples
trait SharafExampleModule extends SharafCommonModule {
  def ivyDeps = Agg(
    ivy"ch.qos.logback:logback-classic:1.4.6"
  )
}

object examples extends mill.Module {
  object api extends SharafExampleModule {
    def moduleDeps = Seq(sharaf)
    object test extends ScalaTests with SharafTestModule
  }
  object fullstack extends SharafExampleModule {
    def moduleDeps = Seq(sharaf)
    object test extends ScalaTests with SharafTestModule
  }
  object oauth2 extends SharafExampleModule {
    def moduleDeps = Seq(sharaf)
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.pac4j:undertow-pac4j:5.0.1",
      ivy"org.pac4j:pac4j-oauth:5.7.0"
    )
    object test extends ScalaTests with SharafTestModule {
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"no.nav.security:mock-oauth2-server:0.5.10"
      )
    }
  }
}

//////////////////// docs
object docs extends MillHepekModule with SharafCommonModule {
  def ivyDeps = Agg(
    ivy"ba.sake::hepek:0.24.1"
  )
}
