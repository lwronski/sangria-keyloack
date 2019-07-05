
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives.{extractCredentials, reject}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import org.keycloak.representations.AccessToken
import pdi.jwt.{Jwt, JwtAlgorithm}
import pdi.jwt.JwtAlgorithm.RS256
import pdi.jwt.algorithms.JwtRSAAlgorithm
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.security.cert.CertificateFactory
import scala.util.Try


trait AuthorizationHandler{


  val publicKeyString = "-----BEGIN PUBLIC KEY-----\nMIIClzCCAX8CBgFrwOHoxjANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDARkZW1vMB4XDTE5MDcwNTA2NDQxOVoXDTI5MDcwNTA2NDU1OVowDzENMAsGA1UEAwwEZGVtbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIr36+r+vH6I6xOUg2ovFPacZodwQDxQmJLLAz/m7vNEx70SOA41GdSA8jkjOHgx84PS+hJWKAH/AnzYU/kpwlR1GVqUk5MuDLvgPJOD0HYZdVFdAYcQc9oKREuwiwzPtL+1hAUqIjsOoGzCOiLqbCTO9Os7mO7sKJLgx5KVrX72WtZDRu79zAc0+c5Y8/RkkNYRBsOYiN7thlcfwfU3rez2oDyqSMin/H0FpnRRwJp2oGH3/fQJereZ/WLI+HTWP6+0kWU5mI+9JZLXkCVB/rVgwMz6lLt6eb4CbTJR8EEAq/eY5EaDGC98L4reLoxc/13nNnJGNrTrxOXqXNBu5DMCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAEZphi87dEFXlGireiDI4Ld8gaUZhqs2hpmniMpKv1s3W3rBfeix9qWGQgvdrXvyyMhbYncwwwZEfegVy4Z6qxkMxQDCV8j/KbZUBZnmylDfR/VTSdvlF8wOpYcK4O9KzYVQqSncqddOaYCoPvX0GjtiIeJKsKjc0qwgRAm+y/FcGuex8s5fLTEoQF4HR91QVfBn2Zl9FpnMDL2wgHbKMST+AxEDYEKo6u54f3Ditru0F+Sm3Rqp5WJuqOL55xORIqQExx7u9Ril4W6Hl3N/yMKiMRE1V7N308Fskutd6G6nBocXtp7QOR3hMymHmrBfF0KcCkB60sUac/V3A+OKVsw==\n-----END PUBLIC KEY-----"

  val json = {
    MyJsonProtocol
  }

  def authorize: Directive1[AccessToken] =
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>

        val publicKey: PublicKey = CertificateFactory
          .getInstance("X.509")
          .generateCertificate(new ByteArrayInputStream(publicKeyString.getBytes))
          .getPublicKey

        println(json.)

        val decodedJwt: Try[String] = Jwt.decode(token, publicKey, Seq(JwtAlgorithm.RS256))
        println(s" decode : $decodedJwt")
        println(s"token $token is valid")
        reject(AuthorizationFailedRejection)
      case _ =>
        println("no token present in request")
        reject(AuthorizationFailedRejection)
    }



}
