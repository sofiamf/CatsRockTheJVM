package authentication

import authentication.HttpDigestDemo.middlewareResource
import cats.effect.*
import com.comcast.ip4s.{ipv4, port}
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import org.http4s.{AuthedRoutes, *}
import org.http4s.implicits.*
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.http4s.headers.Authorization
import org.http4s.headers.Cookie
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.authentication.DigestAuth
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore
import pdi.jwt.JwtAlgorithm
import io.circe.*
import io.circe.parser.*

import java.nio.charset.StandardCharsets
import java.time.LocalTime
import scala.util.Try
import cats.data.*
import org.http4s.server.{AuthMiddleware, Router}

import java.time.Instant
import java.util.Base64

case class User(id: Long, name: String)

object HttpAuthDemo extends IOApp.Simple {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "welcome" / user =>
      Ok(s"Welcome, $user")
  }

  // simple auth method - basic
  // Request[IO] => IO[Either[String, User]]
  // equivalent to Kleisi[IO, Request[IO], Either[String, User]]
  // Kleisli[F, A, B] === A => F[B]
  val basicAuthMethod = Kleisli.apply[IO, Request[IO], Either[String, User]] {
    request =>
      val maybeAuthHeader = request.headers.get[Authorization]
      maybeAuthHeader match {
        case None => IO.pure(Left("No auth header"))
        case Some(Authorization(BasicCredentials(creds))) =>
          IO(Right(User(1L /* fetch from DB */, creds._1)))
        // check the password
        case Some(_) => IO(Left("No basic credentials"))
        case None    => IO(Left("Unauthorized"))
      }
  }

  val onFailure: AuthedRoutes[String, IO] = Kleisli {
    (req: AuthedRequest[IO, String]) =>
      OptionT.pure[IO](Response[IO](status = Status.Unauthorized))
  }

  // middleware
  val userBasicAuthMiddleware: AuthMiddleware[IO, User] =
    AuthMiddleware(basicAuthMethod, onFailure)

  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user =>
      Ok(s"Welcome, $user")
  }

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(
      userBasicAuthMiddleware(authRoutes).orNotFound
    ) // routes.orNotFound
    .build

  override def run =
    server.use(_ => IO.never).void

}

object HttpDigestDemo extends IOApp.Simple {

  val searchFunc: String => IO[Option[(User, String)]] =
    // query
    {
      case "daniel" =>
        for {
          user <- IO.pure(User(1L, "daniel"))
          hash <- Md5HashedAuthStore.precomputeHash[IO]("daniel", "http://localhost:8080","rockthejvm")
        } yield Some(user, hash)
// need to return IO(Some(User(1, Daniel), hash of Daniel))
      case _ => IO.pure(None)
    }

  val authStore = Md5HashedAuthStore(searchFunc)
  val middleware: IO[AuthMiddleware[IO, User]] =
    DigestAuth.applyF[IO, User]("http://localhost:8080/", authStore)

  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user =>
      Ok(s"Welcome, $user")
  }

  val middlewareResource = Resource.eval(middleware)

  val serverResource = for {
    mw <- middlewareResource
    sv <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(mw(authRoutes).orNotFound)
      .build
  } yield sv

  override def run =
    serverResource.use(_ => IO.never).void

}

// 3 - sessions
/*
  1 - user will log in with user/pass
  2 - server replies with a Set-Cookie _____
  3 - user will send further HTTP requests with that cookie - server will accept/deny requests
 */

object HttpSessionDemo extends IOApp.Simple {

  val today: String = LocalTime.now().toString
  def setToken(user: String, date: String) =
    Base64.getEncoder.encodeToString(
      s"$user:$date".getBytes(StandardCharsets.UTF_8)
    )

  def getUser(token: String): Option[String] = Try(new String(Base64.getDecoder.decode(token)).split(":")(0)).toOption

  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => // localhost:8080/welcome/daniel
      Ok(s"Welcome, $user").map(
        _.addCookie(
          ResponseCookie(
            "sessionCookie",
            setToken(user.name, today),
            maxAge = Some(3600)
          )
        )
      )
  }

  val searchFunc: String => IO[Option[(User, String)]] =
    // query
    {
      case "daniel" =>
        for {
          user <- IO.pure(User(1L, "daniel"))
          hash <- Md5HashedAuthStore
            .precomputeHash[IO]("daniel", "http://localhost:8080", "rockthejvm")
        } yield Some(user, hash)
      // need to return IO(Some(User(1, Daniel), hash of Daniel))
      case _ => IO.pure(None)
    }

  val authStore = Md5HashedAuthStore(searchFunc)
  val middleware: IO[AuthMiddleware[IO, User]] =
    DigestAuth.applyF[IO, User]("http://localhost:8080/", authStore)
  val middlewareResource = Resource.eval(middleware)

  // digest auth end

  val cookieAccessRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "statement" =>
      Ok(s"Here is your statement")
    case GET -> Root / "logout" =>
      Ok("Logged out").map(_.removeCookie("sessionCookie"))
  }

  def checkSessionCookie(cookie: Cookie): Option[RequestCookie] = {
    cookie.values.toList.find(_.name == "sessionCookie")
  }

  def modifyPath(user: String): Path =
    Uri.Path.unsafeFromString(s"/statement/$user")

  // prove that the user has a cookie
  def cookieCheckerApp(app: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli { req =>
    val authHeader: Option[Cookie] = req.headers.get[Cookie]
    OptionT.liftF(authHeader.fold(Ok("No token")) { cookie =>
      checkSessionCookie(cookie).fold(Ok("Invalid token")) { token =>
        getUser(token.content).fold(Ok("Invalid token")) { user =>
          // value that we want
          app.orNotFound.run(req.withPathInfo(modifyPath(user)))
        }
      }
    })
  }

  val routerResource = middlewareResource.map { mw =>
    Router(
      "/login" -> mw(authRoutes), // login endpoint (unauthed)
      "/" -> cookieCheckerApp(cookieAccessRoutes) // authed
    )
  }

  val serverResource = for {
    router <- routerResource
    server <-
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(router.orNotFound)
        .build
  } yield server

  override def run =
    serverResource.use(_ => IO.never).void
}

// 4 - JWT
/*object JwtSessionDemo extends IOApp.Simple {

  // "login" endpoints
  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => // localhost:8080/welcome/daniel
      setToken(user.name, new StringBuilder)
      Ok(s"Welcome, $user").map(
        _.addCookie(
          ResponseCookie("token", token)
        )
      )
  }

  case class TokenPayload(user: String, permsLevel: String)

  object TokenPayload {
    given decoder: Decoder[TokenPayload] = Decoder.instance { hCursor =>
      for {
        user <- hCursor.get[String]("user")
        permsLevel <- hCursor.get[String]("level")
      } yield TokenPayload(user, permsLevel)
    }
  }

  // JWT logic
  // claims
  def claim(user: String, permsLevel: String) = JwtClaim(
    content = s"""
        |{
        |  "user": "$user",
        |  "level: "$permsLevel"
        |  }
        |""".stripMargin,
    expiration = Some(Instant.now().plusSeconds(10 * 24 * 3600).getEpochSecond),
    issuedAt = Some(Instant.now().getEpochSecond)
  )

  val key = "tobeconfigured"
  val algo = JwtAlgorithm.HS256
  val token = JwtCirce.encode(claim, key, algo) // build a manual JWT

  // "database"
  val database = Map("daniel" -> User(1L, "Daniel"))

  val authorizedFunction: JwtToken => JwtClaim => IO[Option[User]] =
    token => claim => decode[TokenPayload] match {
      case Left(_) => IO(None)
      case Right(payload) => IO(database.get(payload.user))
    }

  val middleware = JwtAuthMiddleware[IO, User](JwtAuth.hmMAC(key, algo), authorizedFunction)

  val routerResource = middlewareResource.map { mw =>
    Router(
      "/login" -> mw(authRoutes), // login endpoint (unauthed)
      "/guarded" -> middleware(guardedRoutes) // authed
    )
  }

  val guardedRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "secret" as user => // will now be parsed from the JWT
      Ok(s"THIS IS THE SECRET, $user")
  }

  val serverResource = for {
    router <- routerResource
    server <-
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(router.orNotFound)
        .build
  } yield server

  override def run =
    serverResource.use(_ => IO.never).void
}*/
