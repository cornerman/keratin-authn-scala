package authn.frontend

import authn.frontend.authnJS.keratinAuthn.anon.{CurrentPassword, Password, Token}
import authn.frontend.authnJS.keratinAuthn.distCookieSessionStoreMod.CookieSessionStoreOptions
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials
import cats.effect.Async
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.URIUtils

sealed trait SessionStorage
object SessionStorage {
  case class LocalStorage(sessionName: String)                                              extends SessionStorage
  case class Cookie(sessionName: String, options: Option[CookieSessionStoreOptions] = None) extends SessionStorage
  case object Empty                                                                         extends SessionStorage
}

case class AuthnClientConfig(
  hostUrl: String,
  sessionStorage: SessionStorage = SessionStorage.Empty,
)

class AuthnClient[F[_]](config: AuthnClientConfig)(implicit F: Async[F]) {
  import authnJS.keratinAuthn.{mod => authn}

  // TODO: this should not be the global authn singleton, but currently not exported properly
  authn.setHost(config.hostUrl)
  config.sessionStorage match {
    case SessionStorage.LocalStorage(sessionName)          => authn.setLocalStorageStore(sessionName)
    case SessionStorage.Cookie(sessionName, Some(options)) => authn.setCookieStore(sessionName, options)
    case SessionStorage.Cookie(sessionName, None)          => authn.setCookieStore(sessionName)
    case SessionStorage.Empty                              => ()
  }

  def transformAsync[A](f: authn.type => js.Thenable[A]): F[A] = F.fromThenable(F.delay(f(authn)))
  def transformSync[A](f: authn.type => A): F[A]               = F.delay(f(authn))

  def changePassword(args: CurrentPassword): F[Unit]  = transformAsync(_.changePassword(args))
  def importSession: F[Unit]                          = transformAsync(_.importSession())
  def isAvailable(username: String): F[Boolean]       = transformAsync(_.isAvailable(username))
  def login(credentials: Credentials): F[Unit]        = transformAsync(_.login(credentials))
  def logout: F[Unit]                                 = transformAsync(_.logout())
  def requestPasswordReset(username: String): F[Unit] = transformAsync(_.requestPasswordReset(username))
  def requestSessionToken(username: String): F[Unit]  = transformAsync(_.requestSessionToken(username))
  def resetPassword(args: Password): F[Unit]          = transformAsync(_.resetPassword(args))
  def restoreSession: F[Unit]                         = transformAsync(_.restoreSession())
  def sessionTokenLogin(args: Token): F[Unit]         = transformAsync(_.sessionTokenLogin(args))
  def signup(credentials: Credentials): F[Unit]       = transformAsync(_.signup(credentials))
  def session: F[Option[String]]                      = transformSync(_.session().toOption)

  // https://github.com/keratin/authn-server/blob/main/docs/api.md#begin-oauth
  def beginOAuthUrl(providerName: String, redirectUri: String): String = {
    val encodedRedirectUri = URIUtils.encodeURIComponent(redirectUri)
    s"${config.hostUrl.stripSuffix("/")}/oauth/${providerName}?redirect_uri=${encodedRedirectUri}"
  }

  def beginOAuth(providerName: String, redirectUri: String): F[Unit] = F.delay {
    dom.window.location.href = beginOAuthUrl(providerName, redirectUri)
  }
}
