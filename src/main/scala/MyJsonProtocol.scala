import spray.json._
import DefaultJsonProtocol._
import net.liftweb.json.Formats
import net.liftweb.json.JsonAST.{JArray, JString}

// case class KeycloakParser (keys: Array[Map[String,String] | Map[String, Array[String]]])
// case class KeycloakParser (keys: Array[Map[String,String], Map[String,String],Map[String,String], Map[String,String], Map[String,String], Map[String,String], Array[String], Map[String,String], _])
// case class KeycloadField (Map[String, String] | Map[String, [String]])
// case class KeycloakParser (keys: Arr/ay[KeycloadField])
// case class KeycloakParser (keys: Array[Map[String,Array[String]]])
case class KeycloakParser[A] (keys: Array[Map[String,A]])
case class Token(word: List[JString])

object MyJsonProtocol extends DefaultJsonProtocol {

  implicit val formats: Formats = net.liftweb.json.DefaultFormats

  def parseKeycloak(json: String): String = {
    val jsonObj = net.liftweb.json.parse(json)

    val tokens = for {
      tokenList <- (jsonObj \\ "x5c").children
      JArray(subTokenList) <- tokenList
      liftToken <- subTokenList
    } yield liftToken

    for( i <- tokens) {
      println(i.values.toString)
    }

    ""
  }
}