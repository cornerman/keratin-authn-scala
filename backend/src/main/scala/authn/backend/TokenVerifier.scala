package authn.backend

import cats.implicits._
import cats.effect.Sync
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

private class TokenVerifier[F[_]](jwkUrl: String, audiences: Set[String], keychainTTLMinutes: Int)(implicit F: Sync[F]) {
  private val provider =
    new JwkProviderBuilder(jwkUrl).cached(10, keychainTTLMinutes.toLong, TimeUnit.MINUTES).rateLimited(10, 1, TimeUnit.MINUTES).build()

  def verify(token: String): F[String] = for {
    decodedJWT  <- F.delay(JWT.decode(token))
    jwk         <- F.blocking(provider.get(decodedJWT.getKeyId))
    algorithm    = Algorithm.RSA256(jwk.getPublicKey.asInstanceOf[RSAPublicKey], null)
    verifier     = JWT.require(algorithm).withAudience(audiences.toSeq: _*).build()
    verifiedJWT <- F.delay(verifier.verify(decodedJWT))
  } yield verifiedJWT.getPayload
}
