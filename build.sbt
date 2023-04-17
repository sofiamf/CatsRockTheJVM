ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "CatsRockTheJVM"
  )

val catsVersion = "2.1.1"
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
)