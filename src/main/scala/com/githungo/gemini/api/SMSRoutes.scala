package com.githungo.gemini.api

import scala.concurrent.duration._

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import com.githungo.gemini.actors.{SMSResponse, SMSSupervisor}
import com.githungo.gemini.domains.JsonSupport

class SMSRoutes(smsSupervisor: ActorRef[SMSSupervisor.Command])(
  implicit system: ActorSystem[_]
) extends SprayJsonSupport
    with JsonSupport {

  implicit val timeout: Timeout = 30.seconds

  val routes: Route =
    path("sms" / "incoming") {
      post {
        formFields("from", "text", "to".?, "id".?) {
          (from, text, toOpt, idOpt) =>
            val responseFuture = smsSupervisor.ask[SMSResponse](ref =>
              SMSSupervisor.HandleMessage(from, text, ref)
            )

            onSuccess(responseFuture) {
              case SMSResponse.Sent(message) =>
                complete(StatusCodes.OK, s"Response sent: $message")
              case SMSResponse.Failed(error) =>
                complete(StatusCodes.InternalServerError, s"Failed: $error")
            }
        }
      }
    } ~
      path("sms" / "delivery-reports") {
        post {
          formFields("id", "status") { (messageId, status) =>
            // Log delivery status
            system.log.info(
              s"SMS delivery report: messageId=$messageId, status=$status"
            )
            complete(StatusCodes.OK)
          }
        }
      }

}
