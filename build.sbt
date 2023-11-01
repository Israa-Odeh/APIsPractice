ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "APIsPractice",

      // To include akka and Json libraries.
      libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.9.0-M2",
      "com.typesafe.akka" %% "akka-stream" % "2.9.0-M2",
      "com.typesafe.akka" %% "akka-http" % "10.6.0-M1",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.6.0-M1",
      "com.typesafe.play" %% "play-json" % "2.10.2",
    )
  )
