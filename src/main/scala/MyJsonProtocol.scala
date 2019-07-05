import net.liftweb.json.Formats
import net.liftweb.json.JsonAST.JArray
import pdi.jwt.algorithms.JwtRSAAlgorithm
import spray.json._

case class KeycloakParser[A] (keys: Array[Map[String,A]])


object MyJsonProtocol extends DefaultJsonProtocol {




  implicit val formats: Formats = net.liftweb.json.DefaultFormats


  def parseKeycloak(json: String): List[String] = {
    val jsonObj = net.liftweb.json.parse(json)

    val tokens = for {
      tokenList <- (jsonObj \\ "x5c").children
      JArray(subTokenList) <- tokenList
      liftToken <- subTokenList
    } yield liftToken

    for{
      token <- tokens
    } yield token.values.toString
  }

}