package com.githungo.gemini.actors

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike

import com.githungo.gemini.domains._
import com.githungo.gemini.services.{AfricasTalkingService, GeminiService}

class CallSessionActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  // Mock services
  class MockGeminiService extends GeminiService("test-key") {
    override def transcribeAudio(audioUrl: String): Future[String] =
      Future.successful("Hello, I need help with my order")

    override def generateResponse(
      messages: List[ConversationMessage]
    ): Future[String] =
      Future.successful("I'd be happy to help you with your order. What's your order number?")
  }

  class MockAfricasTalkingService extends AfricasTalkingService("test-key", "test-user") {
    override def sendSms(to: String, message: String): Future[String] =
      Future.successful("Message sent successfully")
  }

  "CallSessionActor" should {

    "process recording and generate AI response" in {
      val geminiService = new MockGeminiService()
      val atService = new MockAfricasTalkingService()

      val actor = testKit.spawn(
        CallSessionActor("call-123", "+254712345678", geminiService, atService)
      )
      val probe = testKit.createTestProbe[VoiceResponse]()

      actor ! CallSessionActor.ProcessRecording(
        "http://example.com/recording.wav",
        probe.ref
      )

      val response = probe.receiveMessage(5.seconds)
      response shouldBe a[SayAndRecord]

      val sayAndRecord = response.asInstanceOf[SayAndRecord]
      sayAndRecord.text should include("help you with your order")
    }

    "handle DTMF input for agent transfer" in {
      val geminiService = new MockGeminiService()
      val atService = new MockAfricasTalkingService()

      val actor = testKit.spawn(
        CallSessionActor("call-456", "+254712345678", geminiService, atService)
      )
      val probe = testKit.createTestProbe[VoiceResponse]()

      actor ! CallSessionActor.ProcessDTMF("0", probe.ref)

      val response = probe.receiveMessage(3.seconds)
      response shouldBe a[TransferCall]
    }

    "handle DTMF input for call hangup" in {
      val geminiService = new MockGeminiService()
      val atService = new MockAfricasTalkingService()

      val actor = testKit.spawn(
        CallSessionActor("call-789", "+254712345678", geminiService, atService)
      )
      val probe = testKit.createTestProbe[VoiceResponse]()

      actor ! CallSessionActor.ProcessDTMF("9", probe.ref)

      val response = probe.receiveMessage(3.seconds)
      response shouldBe a[Hangup]
    }

    "handle end call request" in {
      val geminiService = new MockGeminiService()
      val atService = new MockAfricasTalkingService()

      val actor = testKit.spawn(
        CallSessionActor("call-end", "+254712345678", geminiService, atService)
      )
      val probe = testKit.createTestProbe[VoiceResponse]()

      actor ! CallSessionActor.EndCall(probe.ref)

      val response = probe.receiveMessage(3.seconds)
      response shouldBe a[Hangup]
    }

  }

}
