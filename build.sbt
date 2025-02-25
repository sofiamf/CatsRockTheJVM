ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

val Http4sVersion = "0.23.27"
val JwtHtpp4sVersion = "1.2.3"
val JwtScalaVersion = "10.0.1"
val CirceVersion = "0.14.6"
val CirisVersion = "3.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "CatsRockTheJVM"
  )

val catsVersion = "3.5.4"
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % catsVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.http4s" %% "http4s-ember-server" % Http4sVersion,
  "org.http4s" %% "http4s-ember-client" % Http4sVersion,
  "dev.profunktor" %% "http4s-jwt-auth" % JwtHtpp4sVersion,
  "com.github.jwt-scala" %% "jwt-core" % JwtScalaVersion,
  "com.github.jwt-scala" %% "jwt-circe" % JwtScalaVersion,
  "is.cir" %% "ciris" % CirisVersion,
  "is.cir" %% "ciris-circe" % CirisVersion
)
