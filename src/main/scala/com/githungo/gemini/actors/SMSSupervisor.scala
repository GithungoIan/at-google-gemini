package com.githungo.gemini.actors

import scala.collection.mutable

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import com.githungo.gemini.services.{AfricasTalkingService, GeminiService}

object SMSSupervisor {

  sealed trait Command

  case class HandleMessage(
    phoneNumber: String,
    text: String,
    replyTo: ActorRef[SMSResponse]
  ) extends Command

  case class EndSession(phoneNumber: String) extends Command

  def apply(
    geminiService: GeminiService,
    atService: AfricasTalkingService
  ): Behavior[Command] = Behaviors.setup { context =>
    val sessions = mutable.Map.empty[String, ActorRef[
      SMSConversationActor.Command
    ]]

    Behaviors.receiveMessage {

      case HandleMessage(phoneNumber, text, replyTo) =>
        // Get or create session actor for this phone number
        val sessionActor = sessions.getOrElseUpdate(
          phoneNumber,
          context.spawn(
            SMSConversationActor(phoneNumber, geminiService, atService),
            s"sms-session-${phoneNumber.replace("+", "").replace(" ", "")}"
          )
        )

        // Forward message to session actor
        sessionActor ! SMSConversationActor.ProcessMessage(text, replyTo)
        Behaviors.same

      case EndSession(phoneNumber) =>
        sessions.get(phoneNumber).foreach { sessionActor =>
          context.stop(sessionActor)
          sessions.remove(phoneNumber)
        }
        Behaviors.same
    }
  }

}
