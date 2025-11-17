package com.githungo.gemini.actors

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import com.githungo.gemini.domains._
import com.githungo.gemini.services.{AfricasTalkingService, GeminiService}

class SMSConversationActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  // Mock services
  class MockGeminiService extends GeminiService("test-key") {
    override def generateResponse(
      messages: List[ConversationMessage]
    ): Future[String] =
      Future.successful("Hi! How can I help you today?")
  }

  class MockAfricasTalkingService extends AfricasTalkingService("test-key", "test-user") {
    override def sendSms(to: String, message: String): Future[String] =
      Future.successful("Message sent successfully")
  }

  "SMSConversationActor" should {

    "process incoming SMS and send AI response" in {
      val geminiService = new MockGeminiService()
      val atService = new MockAfricasTalkingService()

      val actor = testKit.spawn(
        SMSConversationActor("+254712345678", geminiService, atService)
      )
      val probe = testKit.createTestProbe[SMSResponse]()

      actor ! SMSConversationActor.ProcessMessage("Hello", probe.ref)

      val response = probe.receiveMessage(5.seconds)
      response shouldBe a[SMSResponse.Sent]

      val sentResponse = response.asInstanceOf[SMSResponse.Sent]
      sentResponse.message should include("How can I help")
    }

    "maintain conversation history" in {
      val geminiService = new MockGeminiService()
      val atService = new MockAfricasTalkingService()

      val actor = testKit.spawn(
        SMSConversationActor("+254712345678", geminiService, atService)
      )
      val responseProbe = testKit.createTestProbe[SMSResponse]()
      val historyProbe = testKit.createTestProbe[List[ConversationMessage]]()

      // Send first message
      actor ! SMSConversationActor.ProcessMessage("Hello", responseProbe.ref)
      responseProbe.receiveMessage(5.seconds)

      // Get history
      actor ! SMSConversationActor.GetHistory(historyProbe.ref)
      val history = historyProbe.receiveMessage(3.seconds)

      // Should have system message + user message + assistant response
      history.length should be >= 3
      history.exists(_.role == MessageRole.System) shouldBe true
      history.exists(_.role == MessageRole.User) shouldBe true
      history.exists(_.role == MessageRole.Assistant) shouldBe true
    }

  }

}
