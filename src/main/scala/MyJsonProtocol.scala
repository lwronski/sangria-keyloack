import spray.json._
import DefaultJsonProtocol._

// case class KeycloakParser (keys: Array[Map[String,String] | Map[String, Array[String]]])
// case class KeycloakParser (keys: Array[Map[String,String], Map[String,String],Map[String,String], Map[String,String], Map[String,String], Map[String,String], Array[String], Map[String,String], _])
// case class KeycloadField (Map[String, String] | Map[String, [String]])
// case class KeycloakParser (keys: Arr/ay[KeycloadField])
// case class KeycloakParser (keys: Array[Map[String,Array[String]]])
case class KeycloakParser[A] (keys: Array[Map[String,A]])


object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val keycloakFormat = jsonFormat1(KeycloakParser)
  val testJson = """{"keys":[{
      "kid":"HNrcbp0hqxhmS-LxeRjRhzoEH9vwFlIblQhVeAMUyNk",
      "kty":"RSA",
      "alg":"RS256",
      "use":"sig",
      "n":"jXl_KH9G0tHG8VZh1fGAdK-fPLs_EIqzN2Jp0Nrdixr6aNGNyPhgUmwlGyyy61motF3F_TN_Coj8IRf0_Ztoxw3h6XLZdiFbIIDvEncLfufUnNwWrSwcI9E8mBRf4KBEGeFgLLSvDCxBHJoM6S-T2ea9ueARvzXKvw_YA4YXPE-RSFrtQtyvGDMgabI6uJG5IMw9p__v5IlcO3YFbGKhuTN9nZVLlGQPoC_WXAdypTXdm9t_yhZ4YRrL7snhe9fdYznhkdeLG0UdrOX6agxfGqawV5puVjhDN_TKkde9Cg5IVJPt8z-LSA6n6PZzIZO1FJGYE8qP2Ce9kLSESrtrwQ",
      "e":"AQAB"
  }]}"""

  def parseKeycloak(json: String): KeycloakParser = json.parseJson.convertTo[KeycloakParser]
}