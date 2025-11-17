package com.githungo.gemini.actors

import scala.concurrent.duration._

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import com.githungo._
import gemini.domains._
import gemini.services.{AfricasTalkingService, GeminiService}

object CallSessionActor {
  sealed trait Command

  case class ProcessRecording(
    recordingUrl: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  case class ProcessDTMF(
    digits: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  case class EndCall(replyTo: ActorRef[VoiceResponse]) extends Command

  private case class GeminiResponse(
    text: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  private case class GeminiFailed(
    error: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  private case class TranscriptionComplete(
    transcription: String,
    recordingUrl: String,
    replyTo: ActorRef[VoiceResponse]
  ) extends Command

  private case object SessionTimeout extends Command

  def apply(
    callId: String,
    phoneNumber: String,
    geminiService: GeminiService,
    atService: AfricasTalkingService
  ): Behavior[Command] =
    Behaviors.setup { context =>
      val session = CallSession(
        callId      = callId,
        phoneNumber = phoneNumber,
        state       = CallState.Active,
        messages = List(
          ConversationMessage(
            id   = java.util.UUID.randomUUID(),
            role = MessageRole.System,
            content =
              "You are a helpful customer service assistant. " +
                "Be concise and friendly. If you cannot help, offer to " +
                "transfer to a human agent."
          )
        ),
        startedAt = java.time.Instant.now()
      )

      // Set session timeout
      context.scheduleOnce(5.minutes, context.self, SessionTimeout)

      active(session, geminiService, atService, context)
    }

  private def active(
    session: CallSession,
    geminiService: GeminiService,
    atService: AfricasTalkingService,
    context: ActorContext[Command]
  ): Behavior[Command] = {
    Behaviors.receiveMessage {

      case ProcessRecording(recordingUrl, replyTo) =>
        context.log.info(s"Processing recording for call ${session.callId}")

        // Download and transcribe audio
        context.pipeToSelf(
          geminiService.transcribeAudio(recordingUrl)
        ) {
          case scala.util.Success(transcription) =>
            TranscriptionComplete(transcription, recordingUrl, replyTo)
          case scala.util.Failure(ex) =>
            context.log.error(s"Transcription failed: ${ex.getMessage}")
            GeminiFailed(ex.getMessage, replyTo)
        }
        Behaviors.same

      case TranscriptionComplete(transcription, recordingUrl, replyTo) =>
        context.log.info(s"Transcription: $transcription")

        // Add user message to session
        val userMessage = ConversationMessage(
          id       = java.util.UUID.randomUUID(),
          role     = MessageRole.User,
          content  = transcription,
          audioUrl = Some(recordingUrl)
        )
        val updatedSession = session.addMessage(userMessage)

        // Get AI response
        context.pipeToSelf(
          geminiService.generateResponse(updatedSession.messages)
        ) {
          case scala.util.Success(response) => GeminiResponse(response, replyTo)
          case scala.util.Failure(ex)       => GeminiFailed(ex.getMessage, replyTo)
        }

        active(updatedSession, geminiService, atService, context)

      case GeminiResponse(aiText, replyTo) =>
        context.log.info(s"AI Response: $aiText")

        // Add assistant message
        val assistantMessage = ConversationMessage(
          id      = java.util.UUID.randomUUID(),
          role    = MessageRole.Assistant,
          content = aiText
        )
        val updatedSession = session.addMessage(assistantMessage)

        // Check if user wants to speak to agent
        if (
          aiText.toLowerCase.contains("transfer") ||
          aiText.toLowerCase.contains("speak to agent")
        ) {
          replyTo ! TransferCall("+1234567890", aiText)
        } else {
          // Continue conversation
          replyTo ! SayAndRecord(
            text      = aiText,
            maxLength = 30,
            timeout   = 5
          )
        }

        active(updatedSession, geminiService, atService, context)

      case GeminiFailed(error, replyTo) =>
        context.log.error(s"Gemini failed: $error")
        replyTo ! Say(
          "I apologize, but I'm experiencing technical difficulties. " +
            "Let me transfer you to an agent."
        )
        Behaviors.same

      case ProcessDTMF(digits, replyTo) =>
        context.log.info(s"DTMF received: $digits")

        digits match {
          case "0" =>
            // Transfer to agent
            replyTo ! TransferCall(
              "+1234567890",
              "Transferring you to an agent"
            )
          case "9" =>
            // End call
            replyTo ! Hangup("Thank you for calling. Goodbye!")
            Behaviors.stopped
          case _ => replyTo ! Say("Invalid option. Press 0 for agent, 9 to end call.")
        }
        Behaviors.same

      case EndCall(replyTo) =>
        context.log.info(s"Call ${session.callId} ended")
        val endedSession = session.copy(
          state   = CallState.Completed,
          endedAt = Some(java.time.Instant.now())
        )

        // Save session to database
        // saveSession(endedSession)

        replyTo ! Hangup("Thank you for calling. Goodbye!")
        Behaviors.stopped

      case SessionTimeout =>
        context.log.warn(s"Session timeout for call ${session.callId}")
        Behaviors.stopped
    }
  }

}
