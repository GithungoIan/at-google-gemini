package com.githungo.gemini.actors

import scala.concurrent.duration._

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import com.githungo._
import gemini.domains._
import gemini.services.{AfricasTalkingService, GeminiService}

object SMSConversationActor {

  sealed trait Command

  case class ProcessMessage(
    text: String,
    replyTo: ActorRef[SMSResponse]
  ) extends Command

  case class GetHistory(replyTo: ActorRef[List[ConversationMessage]])
      extends Command

  private case class GeminiResponse(
    text: String,
    replyTo: ActorRef[SMSResponse]
  ) extends Command

  private case class GeminiFailed(
    error: String,
    replyTo: ActorRef[SMSResponse]
  ) extends Command

  private case class SMSSent(replyTo: ActorRef[SMSResponse]) extends Command

  private case class SMSFailed(error: String, replyTo: ActorRef[SMSResponse])
      extends Command

  private case object IdleTimeout extends Command

  // Domain models for SMS
  case class SMSSession(
    id: java.util.UUID,
    phoneNumber: String,
    messages: List[ConversationMessage],
    startedAt: java.time.Instant,
    endedAt: Option[java.time.Instant] = None
  ) {

    def addMessage(message: ConversationMessage): SMSSession =
      copy(messages = messages :+ message)

  }

  def apply(
    phoneNumber: String,
    geminiService: GeminiService,
    atService: AfricasTalkingService
  ): Behavior[Command] = Behaviors.setup { context =>
    val session = SMSSession(
      id = java.util.UUID.randomUUID(),
      phoneNumber = phoneNumber,
      messages = List(
        ConversationMessage(
          id = java.util.UUID.randomUUID(),
          role = MessageRole.System,
          content =
            "You are a helpful SMS assistant. Keep responses under 160 characters. Be concise and friendly."
        )
      ),
      startedAt = java.time.Instant.now()
    )

    // Auto-close after 30 minutes of inactivity
    context.scheduleOnce(30.minutes, context.self, IdleTimeout)

    active(session, geminiService, atService, context)
  }

  private def active(
    session: SMSSession,
    geminiService: GeminiService,
    atService: AfricasTalkingService,
    context: ActorContext[Command]
  ): Behavior[Command] = {
    Behaviors.receiveMessage {

      case ProcessMessage(text, replyTo) =>
        context.log.info(s"SMS from ${session.phoneNumber}: $text")

        // Add user message
        val userMessage = ConversationMessage(
          id = java.util.UUID.randomUUID(),
          role = MessageRole.User,
          content = text
        )
        val updatedSession = session.addMessage(userMessage)

        // Get AI response
        context.pipeToSelf(
          geminiService.generateResponse(updatedSession.messages)
        ) {
          case scala.util.Success(response) =>
            GeminiResponse(response, replyTo)
          case scala.util.Failure(ex) => GeminiFailed(ex.getMessage, replyTo)
        }

        // Reset idle timeout
        context.scheduleOnce(30.minutes, context.self, IdleTimeout)

        active(updatedSession, geminiService, atService, context)

      case GeminiResponse(aiText, replyTo) =>
        context.log.info(s"AI Response: $aiText")

        // Add assistant message
        val assistantMessage = ConversationMessage(
          id = java.util.UUID.randomUUID(),
          role = MessageRole.Assistant,
          content = aiText
        )
        val updatedSession = session.addMessage(assistantMessage)

        // Send SMS via Africa's Talking
        context.pipeToSelf(
          atService.sendSms(session.phoneNumber, aiText)
        ) {
          case scala.util.Success(_) => SMSSent(replyTo)
          case scala.util.Failure(ex) =>
            SMSFailed(ex.getMessage, replyTo)
        }

        active(updatedSession, geminiService, atService, context)

      case SMSSent(replyTo) =>
        context.log.info(s"SMS sent successfully to ${session.phoneNumber}")
        replyTo ! SMSResponse.Sent(
          session.messages.lastOption
            .map(_.content)
            .getOrElse("No message")
        )
        Behaviors.same

      case SMSFailed(error, replyTo) =>
        context.log.error(s"SMS send failed: $error")
        replyTo ! SMSResponse.Failed(error)
        Behaviors.same

      case GeminiFailed(error, replyTo) =>
        context.log.error(s"Gemini failed: $error")
        replyTo ! SMSResponse.Failed(error)
        Behaviors.same

      case GetHistory(replyTo) =>
        replyTo ! session.messages
        Behaviors.same

      case IdleTimeout =>
        context.log.info(s"Session timeout for ${session.phoneNumber}")
        Behaviors.stopped
    }
  }

}

sealed trait SMSResponse

object SMSResponse {
  case class Sent(message: String) extends SMSResponse

  case class Failed(error: String) extends SMSResponse
}
