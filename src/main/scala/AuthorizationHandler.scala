
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.security.cert.CertificateFactory

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives.{extractCredentials, reject}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import org.keycloak.common.VerificationException
import org.keycloak.representations.AccessToken
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.util.Try

trait AuthorizationHandler {

  def authorize: Directive1[AccessToken] =
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>

        val key = s"-----BEGIN PUBLIC KEY-----\n${MyJsonProtocol.keys.head}\n-----END PUBLIC KEY-----"

        val publicKey: PublicKey = CertificateFactory
          .getInstance("X.509")
          .generateCertificate(new ByteArrayInputStream(key.getBytes))
          .getPublicKey

        val decodedJwt: Try[String] = Jwt.decode(token, publicKey, Seq(JwtAlgorithm.RS256))
        println(s" decode : $decodedJwt")

        if (decodedJwt.isFailure) {
          throw new VerificationException(s"$decodedJwt")
        }
        reject(AuthorizationFailedRejection)
      case _ =>
        println("no token present in request")
        reject(AuthorizationFailedRejection)
    }


}
