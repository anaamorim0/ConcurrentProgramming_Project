//import Dependencies._

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.example"

lazy val root = (project in file("."))
  .settings(
    name := "akka-abp",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.20",
      "org.slf4j" % "slf4j-api" % "1.7.36",        // Adicionado SLF4J
      "ch.qos.logback" % "logback-classic" % "1.2.11" // Adicionado Logback para logging
    )
  )