package authn.backend

import cats.implicits._
import cats.effect.Async
import org.http4s.{BasicCredentials, Headers, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.headers.Authorization
import http4sJsoniter.InputStreamEntityCodec._

// Be roughly compatible with usage in node and go:
// https: //github.com/keratin/authn-node/blob/master/src/Client.ts (assumes int ids)
// https://github.com/keratin/authn-go/blob/master/authn/internal_client.go (assumes string ids)

case class AuthnClientConfig(
  issuer: String,
  audiences: Set[String],
  username: String,
  password: String,
  adminURL: Option[String] = None,
  keychainTTLMinutes: Option[Int] = None,
)

class AuthnClient[F[_]](config: AuthnClientConfig, httpClient: Client[F])(implicit F: Async[F]) {

  val tokenVerifier = new TokenVerifier[F](config.issuer, config.audiences, config.keychainTTLMinutes)

  private def accountURL(id: String, action: Option[String] = None): Uri =
    Uri.unsafeFromString(s"${config.adminURL.getOrElse(config.issuer)}/accounts/$id${action.fold("")("/" + _)}")

  private def authorizationHeaders: Headers = Headers(
    Authorization(BasicCredentials(config.username, config.password))
  )

  def account(id: String): F[Account] = {
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

  def updateAccount(id: String, data: AccountUpdate): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        method = Method.PATCH,
        uri = accountURL(id),
        headers = authorizationHeaders,
      ).withEntity(data)
    )
  }

  def archiveAccount(id: String): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.DELETE,
        accountURL(id),
        headers = authorizationHeaders,
      )
    )
  }

  def lockAccount(id: String): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.PATCH,
        accountURL(id, Some("lock")),
        headers = authorizationHeaders,
      )
    )
  }

  def unlockAccount(id: String): F[Unit] = {
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
          accountURL("import"),
          headers = authorizationHeaders,
        ).withEntity(data)
      )
      .map(_.result)
  }

  def expirePassword(id: String): F[Unit] = {
    httpClient.expect[Unit](
      Request[F](
        Method.PATCH,
        accountURL(id, Some("expire_password")),
        headers = authorizationHeaders,
      )
    )
  }
}
