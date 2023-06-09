import mill._
import mill.scalalib._, scalafmt._, publish._

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

object sharaf extends BaseModule with PublishModule {

  def ivyDeps = Agg(
    ivy"io.undertow:undertow-core:2.3.5.Final",
    ivy"ba.sake::tupson:0.5.1-14-3620d5-DIRTYd4843a04",
    ivy"ba.sake::hepek-components:0.10.0+0-3aaeebf1+20230522-1255-SNAPSHOT",
  )

  def artifactName = "sharaf"

  override def publishVersion: T[String] = VcsVersion.vcsState().format()

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

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::0.7.29"
    )
  }
}

trait BaseModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.3.0-RC6"
  def scalacOptions = super.scalacOptions() ++ Seq(
    "-deprecation",
    "-Yretain-trees"
  )

  def repositoriesTask() = T.task {
    super.repositoriesTask() ++ Seq(
      coursier.maven.MavenRepository("https://jitpack.io")
    )
  }
}

////////////////////
object examples extends mill.Module {
  object html extends BaseModule {
    def moduleDeps = Seq(sharaf)
  }
  object json extends BaseModule {
    def moduleDeps = Seq(sharaf)
  }
  object form extends BaseModule {
    def moduleDeps = Seq(sharaf)
  }
  object todo extends BaseModule {
    def moduleDeps = Seq(sharaf)
  }
}
