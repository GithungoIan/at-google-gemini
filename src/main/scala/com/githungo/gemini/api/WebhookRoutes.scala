package com.githungo.gemini.api

import scala.concurrent.duration._

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import com.githungo._
import gemini.domains._
import gemini.actors.CallSupervisor

class WebhookRoutes(
  callSupervisor: ActorRef[CallSupervisor.Command]
)(
  implicit system: ActorSystem[_]
) extends SprayJsonSupport
  with JsonSupport {

  implicit val timeout: Timeout = 30.seconds

  val routes: Route =
    path("voice" / "events") {
      post {
        entity(as[VoiceEventPayload]) { payload =>
          payload.callSessionState match {

            case "Ringing" =>
              // New call initiated
              val greeting = Say(
                "Hello! Welcome to our AI assistant. " +
                  "Please tell me how I can help you today."
              )
              complete(
                HttpEntity(
                  ContentTypes.`text/xml(UTF-8)`,
                  SayAndRecord(
                    "Hello! How can I help you today?",
                    maxLength = 30
                  ).toXml
                )
              )

            case "InProgress" =>
              // Call is active, process user input
              payload.recordingUrl match {
                case Some(recordingUrl) =>
                  // User has spoken, process the recording
                  val responseFuture = callSupervisor.ask[VoiceResponse](ref =>
                    CallSupervisor.HandleRecording(
                      payload.sessionId,
                      payload.callerNumber,
                      recordingUrl,
                      ref
                    )
                  )

                  onSuccess(responseFuture) { response =>
                    complete(
                      HttpEntity(
                        ContentTypes.`text/xml(UTF-8)`,
                        response.toXml
                      )
                    )
                  }

                case None =>
                  // No recording yet, prompt again
                  complete(
                    HttpEntity(
                      ContentTypes.`text/xml(UTF-8)`,
                      SayAndRecord(
                        "I didn't catch that. Could you please repeat?",
                        maxLength = 30
                      ).toXml
                    )
                  )
              }

            case "Completed" =>
              // Call ended
              callSupervisor ! CallSupervisor.EndCall(
                payload.sessionId,
                payload.callerNumber
              )
              complete(StatusCodes.OK)

            case _ => complete(StatusCodes.BadRequest, "Unknown call state")
          }
        }
      }
    } ~
      path("voice" / "dtmf") {
        post {
          entity(as[VoiceEventPayload]) { payload =>
            payload.dtmfDigits match {
              case Some(digits) =>
                val responseFuture = callSupervisor.ask[VoiceResponse](ref =>
                  CallSupervisor.HandleDTMF(
                    payload.sessionId,
                    payload.callerNumber,
                    digits,
                    ref
                  )
                )

                onSuccess(responseFuture) { response =>
                  complete(
                    HttpEntity(
                      ContentTypes.`text/xml(UTF-8)`,
                      response.toXml
                    )
                  )
                }

              case None => complete(StatusCodes.BadRequest, "No DTMF digits")
            }
          }
        }
      }

}
