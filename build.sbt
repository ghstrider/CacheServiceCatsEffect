import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

ThisBuild / scalaVersion := "2.13.7"
ThisBuild / version := "0.1"
ThisBuild / organization := "com.arya"

// Common dependencies across modules
val commonDependencies = Seq(
  "org.typelevel" %% "cats-mtl" % "1.2.1",
  "co.fs2" %% "fs2-io" % "3.2.1",
  "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
  "com.typesafe" % "config" % "1.4.2",
  "com.github.pureconfig" %% "pureconfig" % "0.17.1",
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.1"
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test
)

// Core module - Key-Value store functionality
lazy val core = (project in file("core"))
  .settings(
    name := "cache-service-core",
    libraryDependencies ++= commonDependencies ++ Seq(
      "dev.profunktor" %% "redis4cats-effects" % "1.0.0",
      "dev.profunktor" %% "redis4cats-log4cats" % "1.0.0"
    ) ++ testDependencies
  )

// CLI module - Command-line interface
lazy val cli = (project in file("cli"))
  .dependsOn(core)
  .settings(
    name := "cache-service-cli",
    libraryDependencies ++= commonDependencies ++ Seq(
      "com.monovore" %% "decline" % "2.2.0",
      "com.monovore" %% "decline-effect" % "2.2.0"
    ) ++ testDependencies,
    assembly / mainClass := Some("com.arya.cli.Cli"),
    assembly / assemblyJarName := "cache-service-cli.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  )
  .enablePlugins(sbtassembly.AssemblyPlugin)

// Root project
lazy val root = (project in file("."))
  .aggregate(core, cli)
  .settings(
    name := "cache-service",
    publish := {},
    publishLocal := {}
  )