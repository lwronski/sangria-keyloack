import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives.{extractCredentials, onComplete, provide, reject}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import org.keycloak.representations.AccessToken
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AuthorizationHandler{


  def authorize: Directive1[AccessToken] =
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>
          onComplete( Future {
            verifyToken(token)
          }
          ).flatMap {
            provide(AdapterTokenVerifier.verifyToken(token, keycloakDeployment))
            _.map(accessToken => provide(accessToken)).recover {
              case e =>
                reject(AuthorizationFailedRejection)
                  .toDirective[Tuple1[AccessToken]]
            }.get
          }
        reject(AuthorizationFailedRejection)
      case _ =>
        println("no token present in request")
        reject(AuthorizationFailedRejection)

    }

  def verifyToken(token: String): AccessToken = {
    AdapterTokenVerifier.verifyToken(token, keycloakDeployment)
  }

  val keycloakDeployment: KeycloakDeployment =
    KeycloakDeploymentBuilder.build(getClass.getResourceAsStream("/assets/keycloak.json"))

}
