import sangria.ast.Document
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.{QueryParser, SyntaxError}
import sangria.parser.DeliveryScheme.Try
import sangria.marshalling.circe._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import GraphQLRequestUnmarshaller._
import akka.http.scaladsl.model.HttpResponse
import sangria.slowlog.SlowLog

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.util.{ Failure, Success }

import akka.util.ByteString

import scala.io._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
import MyJsonProtocol._

object Server extends App with CorsSupport  with AuthorizationHandler {
  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/auth/realms/demo/protocol/openid-connect/certs"))

  responseFuture
    .onComplete {
      case Success(res) => receive(res)
      case Failure(_)   => sys.error("something wrong")
    }
  val jsonKeycloakParser = {
    MyJsonProtocol
  }

  def receive(res: HttpResponse) = res match {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        println("Got response, body: " + body.utf8String)
        val test = jsonKeycloakParser.parseKeycloak(body.utf8String)
      }
    case resp @ HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }

  def executeGraphQL(query: Document, operationName: Option[String], variables: Json, tracing: Boolean) =
    complete(Executor.execute(SchemaDefinition.StarWarsSchema, query, new CharacterRepo,
      variables = if (variables.isNull) Json.obj() else variables,
      operationName = operationName,
      middleware = if (tracing) SlowLog.apolloTracing :: Nil else Nil,
      deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters))
        .map(OK → _)
        .recover {
          case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
          case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
        })

  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError ⇒
      Json.obj("errors" → Json.arr(
      Json.obj(
        "message" → Json.fromString(syntaxError.getMessage),
        "locations" → Json.arr(Json.obj(
          "line" → Json.fromBigInt(syntaxError.originalError.position.line),
          "column" → Json.fromBigInt(syntaxError.originalError.position.column))))))
    case NonFatal(e) ⇒
      formatError(e.getMessage)
    case e ⇒
      throw e
  }

  def formatError(message: String): Json =
    Json.obj("errors" → Json.arr(Json.obj("message" → Json.fromString(message))))

  val route: Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing ⇒
      path("graphql") {
        get {
          explicitlyAccepts(`text/html`) {
            getFromResource("assets/graphiql.html")
          } ~
          parameters('query, 'operationName.?, 'variables.?) { (query, operationName, variables) ⇒
            QueryParser.parse(query) match {
              case Success(ast) ⇒
                variables.map(parse) match {
                  case Some(Left(error)) ⇒ complete(BadRequest, formatError(error))
                  case Some(Right(json)) ⇒ executeGraphQL(ast, operationName, json, tracing.isDefined)
                  case None ⇒ executeGraphQL(ast, operationName, Json.obj(), tracing.isDefined)
                }
              case Failure(error) ⇒ complete(BadRequest, formatError(error))
            }
          }
        } ~
        post {
          authorize { token =>
            complete(BadRequest, formatError("No query to execute"))
          } ~
            parameters('query.?, 'operationName.?, 'variables.?) { (queryParam, operationNameParam, variablesParam) ⇒
            entity(as[Json]) { body ⇒
              val query = queryParam orElse root.query.string.getOption(body)
              val operationName = operationNameParam orElse root.operationName.string.getOption(body)
              val variablesStr = variablesParam orElse root.variables.string.getOption(body)

              query.map(QueryParser.parse(_)) match {
                case Some(Success(ast)) ⇒
                  variablesStr.map(parse) match {
                    case Some(Left(error)) ⇒ complete(BadRequest, formatError(error))
                    case Some(Right(json)) ⇒ executeGraphQL(ast, operationName, json, tracing.isDefined)
                    case None ⇒ executeGraphQL(ast, operationName, root.variables.json.getOption(body) getOrElse Json.obj(), tracing.isDefined)
                  }
                case Some(Failure(error)) ⇒ complete(BadRequest, formatError(error))
                case None ⇒ complete(BadRequest, formatError("No query to execute"))
              }
            } ~
            entity(as[Document]) { document ⇒
              variablesParam.map(parse) match {
                case Some(Left(error)) ⇒ complete(BadRequest, formatError(error))
                case Some(Right(json)) ⇒ executeGraphQL(document, operationNameParam, json, tracing.isDefined)
                case None ⇒ executeGraphQL(document, operationNameParam, Json.obj(), tracing.isDefined)
              }
            }
          }
        }
      } ~
      path("keycloak-json"){
        get {
          explicitlyAccepts(`application/json`) {
            getFromResource("assets/keycloak.json")
          }
        }
      }
    } ~
    (get & pathEndOrSingleSlash) {
      redirect("/graphql", PermanentRedirect)
    }

  Http().bindAndHandle(corsHandler(route), "0.0.0.0", sys.props.get("http.port").fold(3000)(_.toInt))
}
