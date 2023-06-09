import mill._
import mill.scalalib._, scalafmt._, publish._

import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import io.kipp.mill.ci.release.CiReleaseModule

object sharaf extends SharafPublishModule {

  def artifactName = "sharaf"

  def ivyDeps = Agg(
    ivy"io.undertow:undertow-core:2.3.5.Final",
    ivy"ba.sake::tupson:0.6.0",
    ivy"ba.sake::hepek-components:0.10.0+0-3aaeebf1+20230522-1255-SNAPSHOT",
  )

  def moduleDeps = Seq(querson, formson)

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::0.7.29"
    )
  }
}

object querson extends SharafPublishModule {

  def artifactName = "querson"

  def ivyDeps = Agg(
    ivy"ba.sake::tupson:0.6.0",
  )

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::0.7.29"
    )
  }
}

object formson extends SharafPublishModule {

  def artifactName = "formson"

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::0.7.29"
    )
  }
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
  def scalaVersion = "3.3.0"
  def scalacOptions = super.scalacOptions() ++ Seq(
    "-deprecation",
    "-Yretain-trees",
    "-Wunused:all"
  )
}

////////////////////
object examples extends mill.Module {
  object html extends SharafCommonModule {
    def moduleDeps = Seq(sharaf)
  }
  object json extends SharafCommonModule {
    def moduleDeps = Seq(sharaf)
  }
  object form extends SharafCommonModule {
    def moduleDeps = Seq(sharaf)
  }
  object todo extends SharafCommonModule {
    def moduleDeps = Seq(sharaf)
  }
}
