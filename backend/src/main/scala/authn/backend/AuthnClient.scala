package authn.backend

import cats.implicits._
import cats.effect.Async
import org.http4s.{BasicCredentials, Headers, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.headers.Authorization
import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import http4sJsoniter.InputStreamEntityCodec._

// Be roughly compatible with usage in node:
// https: //github.com/keratin/authn-node/blob/master/src/Client.ts
case class AuthnClientConfig(
  issuer: String,
  audiences: Set[String], // authn-node allows String | String[]
  username: String,
  password: String,
  adminURL: Option[String] = None,
  keychainTTLMinutes: Int = 60, // instead of just keychainTTL to be more explicit
)

class AuthnClient[F[_]](config: AuthnClientConfig, httpClient: Client[F])(implicit F: Async[F]) {
  private val verifier = new TokenVerifier[F](config.issuer, config.audiences, config.keychainTTLMinutes)

  private def accountURL(id: Int, action: Option[String] = None): Uri =
    Uri.unsafeFromString(s"${config.adminURL.getOrElse(config.issuer)}/accounts/$id${action.fold("")("/" + _)}")

  private def authorizationHeaders: Headers = Headers(
    Authorization(BasicCredentials(config.username, config.password))
  )

  def account(id: Int): F[Account] = {
    httpClient
      .expect[ServerResponse[Account]](
        Request[F](
          Method.GET,
          accountURL(id),
          headers = authorizationHeaders,
        )
      )
      .map(_.result)
  }

  def updateAccount(id: Int, data: AccountUpdate): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        method = Method.PATCH,
        uri = accountURL(id),
        headers = authorizationHeaders,
      ).withEntity(data)
    )
  }

  def archiveAccount(id: Int): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.DELETE,
        accountURL(id),
        headers = authorizationHeaders,
      )
    )
  }

  def lockAccount(id: Int): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.PATCH,
        accountURL(id, Some("lock")),
        headers = authorizationHeaders,
      )
    )
  }

  def unlockAccount(id: Int): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.PATCH,
        accountURL(id, Some("unlock")),
        headers = authorizationHeaders,
      )
    )
  }

  def importAccount(data: AccountImport): F[AccountImported] = {
    httpClient
      .expect[ServerResponse[AccountImported]](
        Request[F](
          Method.POST,
          Uri.fromString(s"${config.adminURL.getOrElse(config.issuer)}/accounts/import").toOption.get,
          headers = authorizationHeaders,
        ).withEntity(data)
      )
      .map(_.result)
  }

  def expirePassword(id: Int): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.PATCH,
        accountURL(id, Some("expire_password")),
        headers = authorizationHeaders,
      )
    )
  }

  def verifyToken(token: String): F[TokenPayload] = for {
    payloadStr  <- verifier.verify(token)
    payloadJson <- F.delay(readFromString[TokenPayload](payloadStr))
  } yield payloadJson
}
object AuthnClient {
  def apply[F[_]: Async](config: AuthnClientConfig, httpClient: Client[F]): AuthnClient[F] =
    new AuthnClient[F](config, httpClient)
}
