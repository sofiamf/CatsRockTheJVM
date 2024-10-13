package authentication

import io.circe.*
import ciris.*
import ciris.circe.*
import org.http4s.*
import org.http4s.implicits.*
import cats.effect.*
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.ember.client.EmberClientBuilder
import com.comcast.ip4s.*
import org.http4s.headers.{Accept, Authorization}
import io.circe.parser.*
import java.nio.file.Paths

/*
  1. User tries to log in to my application -> click a link
  2. redirect the user to the provider's authorization page
  3. user is redirected to a "callback" page on the small app with a code
  4. (in the backend) server gets data from the auth provider with that code
  5. (on Github) auth server responds with an auth token and some data
  6. my small app shows something to the user depending on the token and the data
 */

object OAuthDemo extends IOApp.Simple {

  private val appConfig = file(Paths.get("src/main/resources/AppConfig.json")).as[AppConfig]
  // ciris data types hashes the string
  case class AppConfig(key: String, secret: Secret[String])
  object AppConfig {
    given appDecoder: Decoder[AppConfig] = Decoder.instance {
      h =>
        for {
          key <- h.get[String]("key")
          secret <- h.get[String]("secret")
        } yield AppConfig(key, Secret(secret))
    }
    given appConfigDecoder: ConfigDecoder[String, AppConfig] = circeConfigDecoder("AppConfig")
  }

  def getOAuthResult(code: String, config: AppConfig): IO[String] =
    for {
      maybeToken <- fetchJsonString(code, config)
      result <- maybeToken match {
        case Some(token) => fetchUserInfo(token)
        case None => IO("Authentication failed")
      }
    } yield result



  // 1 - contact GitHub auth URL (we need to be a client)
  // github.com/login/oauth/access_token
  // URL form into a POST request: code, client_id, client_secret
  // 2 - GitHub gives us back a JSON string
  // 3 - decode that string
  def fetchJsonString(code: String, config: AppConfig): IO[Option[String]] = {
    val form = UrlForm(
      "client_id" -> config.key,
      "client_secret" -> config.secret.value,
      "code" -> code
    )

    val req = Request[IO](
      Method.POST,
      uri"https://github.com/login/oauth/access_token",
      headers = Headers(Accept(MediaType.application.json))
    ).withEntity(form)

    EmberClientBuilder.default[IO].build.use(client => client.expect[String](req))
      .map(jsonString => decode[GitHubTokenRespone](jsonString))
      .map {
        case Left(e) => None
        case Right(gtr) => Some(gtr.accessToken)
      }
  }

  def fetchUserInfo(token: String): IO[String] = {
    val req = Request[IO](
      Method.GET,
      uri"https://api.github.com/user/emails",
      headers = Headers(Accept(MediaType.application.json),
      headers.Authorization(Credentials.Token(AuthScheme.Bearer, token))
      )
    )
    EmberClientBuilder.default[IO].build.use(client => client.expect[String](req)).map { response =>
      decode[List[GitHubUser]](response).toOption.flatMap(_.find(_.primary)) match
        case Some(value) => s"Success!! Logged in as ${value.email}"
        case None => "No primary email found"
    }
  }

  // http://localhost:8080/callback?code=63857d838c8713177006

  val dsl = Http4sDsl[IO]
  import dsl.*
  def routes(config: AppConfig): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "home" =>
      StaticFile.fromString("src/main/resources/html/index.html", Some(req)).getOrElseF(NotFound())
    case GET -> Root / "callback" :? GitHubTokenQueryParamMatcher(code) => 
      getOAuthResult(code, config).flatMap(result => Ok(result))
    // TODO: add Github query parameter
    // TODO validate the code and return success/failure of auth
  }

  override def run: IO[Unit] = for {
      config <- appConfig.load[IO]
      server <- EmberServerBuilder.default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(routes(config).orNotFound)
        .build
        .use(_ => IO(println("Rock the JVM")) *> IO.never)
    } yield()

  private object GitHubTokenQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")

  case class GitHubUser(email: String, primary: Boolean, verified: Boolean) derives Decoder

  case class GitHubTokenRespone(accessToken: String, tokenType: String, scope: String)
  object GitHubTokenRespone {
    given decoder: Decoder[GitHubTokenRespone] = Decoder.instance { h =>
      for {
        accessToken <- h.get[String]("access_token")
        tokenType <- h.get[String]("token_type")
        scope <- h.get[String]("scope")
      } yield GitHubTokenRespone(accessToken, tokenType, scope)
    }
  }

}




