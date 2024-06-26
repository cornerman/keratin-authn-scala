package authn.backend

import cats.implicits._
import cats.effect.Sync
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT

import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit
import java.net.URI

case class VerifiedToken(token: DecodedJWT) {
  def accountId: String = token.getSubject
}

class TokenVerifier[F[_]](issuer: String, audiences: Set[String], adminURL: Option[String] = None, keychainTTLMinutes: Option[Int] = None)(
  implicit F: Sync[F],
) {
  private val provider =
    new JwkProviderBuilder(new URI(s"${adminURL.getOrElse(issuer)}/jwks").toURL)
      .cached(10, keychainTTLMinutes.getOrElse(60).toLong, TimeUnit.MINUTES)
      .rateLimited(10, 1, TimeUnit.MINUTES)
      .build()

  def verify(token: String): F[VerifiedToken] = for {
    decodedJWT  <- F.delay(JWT.decode(token))
    jwk         <- F.blocking(provider.get(decodedJWT.getKeyId))
    algorithm    = Algorithm.RSA256(jwk.getPublicKey.asInstanceOf[RSAPublicKey], null)
    verifier     = JWT.require(algorithm).withIssuer(issuer).withAnyOfAudience(audiences.toSeq: _*).build()
    verifiedJWT <- F.delay(verifier.verify(decodedJWT))
  } yield VerifiedToken(verifiedJWT)
}
