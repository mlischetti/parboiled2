import sbtcrossproject.CrossPlugin.autoImport._

val Scala2_12 = "2.12.17"
val Scala2_13 = "2.13.10"
val Scala3    = "3.2.2"

val isScala3 = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
)

ThisBuild / versionScheme      := Some("early-semver")
ThisBuild / scalaVersion       := Scala3
ThisBuild / crossScalaVersions := Seq(Scala2_12, Scala2_13, Scala3)

val commonSettings = Seq(
  organization := "org.parboiled",
  homepage     := Some(new URL("http://parboiled.org")),
  description  := "Fast and elegant PEG parsing in Scala - lightweight, easy-to-use, powerful",
  startYear    := Some(2009),
  licenses     := Seq("Apache-2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  (Compile / unmanagedResources) += baseDirectory.value.getParentFile.getParentFile / "LICENSE",
  scmInfo := Some(
    ScmInfo(url("https://github.com/sirthias/parboiled2"), "scm:git:git@github.com:sirthias/parboiled2.git")
  ),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked"
    // "-Ywarn-numeric-widen",
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        Seq(
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits,-explicits",
          "-Ycache-macro-class-loader:last-modified",
          "-Ybackend-parallelism",
          "8",
          "-Xfatal-warnings",
          "-Xfuture",
          "-Xlint:_,-missing-interpolator",
          "-Ywarn-dead-code",
          "-Xsource:2.13" // new warning: deprecate assignments in argument position
        )
      case Some((2, 13)) =>
        Seq(
          "-Xlint:_,-missing-interpolator",
          "-Ywarn-dead-code",
          "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits,-explicits",
          "-Ycache-macro-class-loader:last-modified",
          "-Ybackend-parallelism",
          "8"
        )
      case Some((3, _)) =>
        Seq("-language:implicitConversions")
      case x => sys.error(s"unsupported scala version: $x")
    }
  },
  Compile / console / scalacOptions ~= (_ filterNot (o => o == "-Ywarn-unused-import" || o == "-Xfatal-warnings")),
  Test / console / scalacOptions ~= (_ filterNot (o => o == "-Ywarn-unused-import" || o == "-Xfatal-warnings")),
  Compile / doc / scalacOptions += "-no-link-warnings",
  sourcesInBase := false,
  // file headers
  headerLicense := Some(HeaderLicense.ALv2("2009-2019", "Mathias Doenitz"))
)

lazy val nativeSettings = Seq(
)

lazy val scalajsSettings = Seq(
  scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule).withSourceMap(false)),
  Global / scalaJSStage := FastOptStage,
  scalacOptions ~= { _.filterNot(_ == "-Ywarn-dead-code") }
)

lazy val publishingSettings = Seq(
  publishMavenStyle         := true,
  Test / publishArtifact    := false,
  pomIncludeRepository      := (_ => false),
  publishTo                 := sonatypePublishTo.value,
  publishConfiguration      := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  developers := List(
    Developer("sirthias", "Mathias Doenitz", "devnull@bullet.io", url("https://github.com/sirthias")),
    Developer("alexander-myltsev", "Alexander Myltsev", "", url("http://www.linkedin.com/in/alexandermyltsev"))
  )
)

val utestSettings = Seq(testFrameworks := Seq(new TestFramework("utest.runner.Framework")))

lazy val parboiledOsgiSettings = osgiSettings ++ Seq(
  OsgiKeys.exportPackage  := Seq("org.parboiled2.*;version=${Bundle-Version}"),
  OsgiKeys.privatePackage := Seq()
)

/////////////////////// DEPENDENCIES /////////////////////////

val utest           = Def.setting("com.lihaoyi" %%% "utest" % "0.8.1" % Test)
val scalaCheck      = Def.setting("org.scalacheck" %%% "scalacheck" % "1.17.0" % Test)
val `scala-reflect` = Def.setting("org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided)

// benchmarks and examples only
val `json4s-native`  = "org.json4s" %% "json4s-native"  % "4.0.6"
val `json4s-jackson` = "org.json4s" %% "json4s-jackson" % "4.0.6"
val `spray-json`     = "io.spray"   %% "spray-json"     % "1.3.6"

/////////////////////// PROJECTS /////////////////////////

lazy val root = project
  .in(file("."))
  .aggregate(examples, jsonBenchmark)
  .aggregate(parboiledJVM, parboiledJS, parboiledNative)
  .aggregate(parboiledCoreJVM, parboiledCoreJS, parboiledCoreNative)
  .settings(commonSettings)
  .settings(publish / skip := true)
  .settings(
    name := "parboiled2-root"
  )

lazy val examples = project
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(parboiledJVM)
  .settings(commonSettings)
  .settings(utestSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(utest.value, `spray-json`)
  )

lazy val bench = inputKey[Unit]("Runs the JSON parser benchmark with a simple standard config")

lazy val jsonBenchmark = project
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(examples)
  .enablePlugins(JmhPlugin)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(`json4s-native`, `json4s-jackson`),
    bench := (Compile / run).partialInput(" -i 10 -wi 10 -f1 -t1").evaluated
  )

lazy val scalaParser = project
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(parboiledJVM)
  .settings(commonSettings)
  .settings(utestSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(utest.value)
  )

lazy val parboiledJVM    = parboiled.jvm
lazy val parboiledJS     = parboiled.js
lazy val parboiledNative = parboiled.native

lazy val parboiled = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(AutomateHeaderPlugin, SbtOsgi)
  .dependsOn(parboiledCore)
  .settings(commonSettings)
  .settings(publishingSettings)
  .settings(parboiledOsgiSettings)
  .settings(utestSettings)
  .jvmSettings(
    (Compile / packageBin / mappings) ++= (parboiledCoreJVM.project / Compile / packageBin / mappings).value,
    (Compile / packageSrc / mappings) ++= (parboiledCoreJVM.project / Compile / packageSrc / mappings).value,
    (Compile / packageDoc / mappings) ++= (parboiledCoreJVM.project / Compile / packageDoc / mappings).value
  )
  .jsSettings(
    (Compile / packageBin / mappings) ++= (parboiledCoreJS.project / Compile / packageBin / mappings).value,
    (Compile / packageSrc / mappings) ++= (parboiledCoreJS.project / Compile / packageSrc / mappings).value,
    (Compile / packageDoc / mappings) ++= (parboiledCoreJS.project / Compile / packageDoc / mappings).value
  )
  .nativeSettings(
    (Compile / packageBin / mappings) ++= (parboiledCoreNative.project / Compile / packageBin / mappings).value,
    (Compile / packageSrc / mappings) ++= (parboiledCoreNative.project / Compile / packageSrc / mappings).value,
    (Compile / packageDoc / mappings) ++= (parboiledCoreNative.project / Compile / packageDoc / mappings).value
  )
  .settings(
    libraryDependencies ++= {
      if (isScala3.value) Seq(utest.value)
      else Seq(`scala-reflect`.value, utest.value)
    },
    (Compile / packageBin / mappings) ~= (_.groupBy(_._2).toSeq.map(_._2.head)), // filter duplicate outputs
    (Compile / packageDoc / mappings) ~= (_.groupBy(_._2).toSeq.map(_._2.head)), // filter duplicate outputs
    pomPostProcess := { // we need to remove the dependency onto the parboiledCore module from the POM
      import scala.xml.transform._
      import scala.xml.{NodeSeq, Node => XNode}

      val filter = new RewriteRule {
        override def transform(n: XNode) = if ((n \ "artifactId").text.startsWith("parboiledcore")) NodeSeq.Empty else n
      }
      new RuleTransformer(filter).transform(_).head
    }
  )
  .nativeSettings(nativeSettings)

lazy val generateActionOps = taskKey[Seq[File]]("Generates the ActionOps boilerplate source file")

lazy val parboiledCoreJVM    = parboiledCore.jvm
lazy val parboiledCoreJS     = parboiledCore.js
lazy val parboiledCoreNative = parboiledCore.native

lazy val parboiledCore = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("parboiled-core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(utestSettings)
  .settings(libraryDependencies += scalaCheck.value)
  .settings(
    publish / skip := true,
    libraryDependencies ++= {
      if (isScala3.value) Seq(utest.value)
      else Seq(`scala-reflect`.value, utest.value)
    },
    generateActionOps := ActionOpsBoilerplate((Compile / sourceManaged).value, streams.value),
    Compile / sourceGenerators += generateActionOps.taskValue
  )
  .nativeSettings(nativeSettings)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("master"))
  )

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"), JavaSpec.temurin("11"), JavaSpec.temurin("17"))
ThisBuild / githubWorkflowOSes         := Seq("ubuntu-latest", "windows-latest")

// Workaround that is necessary for Windows,
// see https://github.com/scala-native/scala-native/blob/9a185986f10a5e69c32339d5701c1196e9885e17/.github/actions/windows-setup-env/action.yml#L21-L33
ThisBuild / githubWorkflowBuildPreamble := Seq(
  WorkflowStep.Use(
    name = Some("Configure pagefile for Windows"),
    ref = UseRef.Public("al-cheb", "configure-pagefile-action", "v1.3"),
    params = Map(
      "minimum-size" -> "4GB",
      "maximum-size" -> "16GB"
    ),
    cond = Some("contains(runner.os, 'windows')")
  )
)

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("headerCheckAll", "scalaParser/headerCheckAll"),
    name = Some("Header check"),
    cond = {
      // Header check only needs to be run for one JVM, os along with one version of Scala
      val jVersion = (ThisBuild / githubWorkflowJavaVersions).value.head.render
      val sVersion = (ThisBuild / crossScalaVersions).value.head
      val uOs      = (ThisBuild / githubWorkflowOSes).value.find(_.contains("ubuntu")).head
      Some(
        "${{ matrix.java=='" + jVersion + "' && matrix.scala=='" + sVersion + "' && matrix.os=='" + uOs + "' }}"
      )
    }
  ),
  WorkflowStep.Sbt(
    List("test", "scalaParser/testOnly scalaparser.SnippetSpec"),
    name = Some("Build project")
  )
)
