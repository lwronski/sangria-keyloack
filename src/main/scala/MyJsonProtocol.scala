import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import net.liftweb.json.Formats
import net.liftweb.json.JsonAST.JArray
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}


object MyJsonProtocol extends DefaultJsonProtocol {

  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  var keys = List[String]()

  {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/auth/realms/demo/protocol/openid-connect/certs"))

    implicit val formats: Formats = net.liftweb.json.DefaultFormats

    def parseKeycloak(json: String): List[String] = {
      val jsonObj = net.liftweb.json.parse(json)

      val tokens = for {
        tokenList <- (jsonObj \\ "x5c").children
        JArray(subTokenList) <- tokenList
        liftToken <- subTokenList
      } yield liftToken

      for {
        token <- tokens
      } yield token.values.toString
    }

    def receive(res: HttpResponse) = res match {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          keys = parseKeycloak(body.utf8String)

        }
      case resp@HttpResponse(code, _, _, _) =>
        println("Request failed, response code: " + code)
        resp.discardEntityBytes()
    }

    responseFuture
      .onComplete {
        case Success(res) => receive(res)
        case Failure(_) => sys.error("something wrong")
      }

  }

}