
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives.{extractCredentials, reject}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import org.keycloak.representations.AccessToken

trait AuthorizationHandler{


  def authorize: Directive1[AccessToken] =
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>
        println(s"token $token is valid")
        reject(AuthorizationFailedRejection)
      case _ =>
        println("no token present in request")
        reject(AuthorizationFailedRejection)
    }



}