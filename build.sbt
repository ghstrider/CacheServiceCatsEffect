import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

lazy val cacheService = (project in file(".")).settings(
  name := "CacheServiceCatsEffect",
  version := "0.1",
  scalaVersion := "2.13.7"
).settings(
  libraryDependencies ++= Seq( //"org.typelevel" %% "cats-effect" % "3.3.1",
    "org.typelevel" %% "cats-mtl" % "1.2.1",
    //    "org.typelevel" %% "cats-mtl-core" % "0.7.1",
    "co.fs2" %% "fs2-io" % "3.2.1",
    "com.monovore" %% "decline" % "2.2.0",
    "com.monovore" %% "decline-effect" % "2.2.0",
    "dev.profunktor" %% "redis4cats-effects" % "1.0.0",
    "dev.profunktor" %% "redis4cats-log4cats" % "1.0.0",
    "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
    "com.typesafe" % "config" % "1.4.2",
    "com.github.pureconfig" %% "pureconfig" % "0.17.1",
    "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.1",
    "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    "org.scalamock" %% "scalamock" % "5.2.0" % Test,
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test
  ),
  mainClass := Some("com.arya.cli.Cli"),
  assembly / mainClass := Some("com.arya.cli.Cli"),
  assembly / assemblyJarName := "keystore.jar",
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
)
