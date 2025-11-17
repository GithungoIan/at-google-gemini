package com.githungo.gemini.actors

import scala.collection.mutable

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import com.githungo._
import gemini.domains._
import gemini.services.{AfricasTalkingService, GeminiService}

object CallSupervisor {
  sealed trait Command

  case class HandleRecording(
    callId: String,
    phoneNumber: String,
    recordingUrl: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  case class HandleDTMF(
    callId: String,
    phoneNumber: String,
    digits: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  case class EndCall(
    callId: String,
    phoneNumber: String
  ) extends Command

  def apply(
    geminiService: GeminiService,
    atService: AfricasTalkingService
  ): Behavior[Command] =
    Behaviors.setup { context =>
      val sessions = mutable.Map.empty[String, ActorRef[CallSessionActor.Command]]

      Behaviors.receiveMessage {
        case HandleRecording(callId, phoneNumber, recordingUrl, replyTo) =>
          val sessionActor = sessions.getOrElseUpdate(
            callId,
            context.spawn(
              CallSessionActor(callId, phoneNumber, geminiService, atService),
              s"call-session-$callId"
            )
          )

          sessionActor ! CallSessionActor.ProcessRecording(recordingUrl, replyTo)
          Behaviors.same

        case HandleDTMF(callId, phoneNumber, digits, replyTo) =>
          sessions.get(callId) match {
            case Some(sessionActor) =>
              sessionActor ! CallSessionActor.ProcessDTMF(digits, replyTo)
            case None => replyTo ! Say("Session not found. Please call again.")
          }
          Behaviors.same

        case EndCall(callId, phoneNumber) =>
          sessions.get(callId).foreach { sessionActor =>
            context.stop(sessionActor)
            sessions.remove(callId)
          }
          Behaviors.same
      }
    }

}
