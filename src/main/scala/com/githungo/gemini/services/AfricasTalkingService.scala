package com.githungo.gemini.services

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.util.ByteString

class AfricasTalkingService(apiKey: String, username: String)(
  implicit system: ActorSystem,
  ec: ExecutionContext
) {

  private val baseUrl = "https://api.africastalking.com/version1"

  def makeCall(to: String, callbackUrl: String): Future[String] = {
    val formData = FormData(
      "username" -> username,
      "to"       -> to,
      "from"     -> "+254XXXXXXXX" // Your AT phone number
    )

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = s"$baseUrl/voice/call",
      headers = List(
        Authorization(BasicHttpCredentials(username, apiKey))
      ),
      entity = formData.toEntity
    )

    Http()
      .singleRequest(request)
      .flatMap { response =>
        response
          .entity
          .dataBytes
          .runFold(ByteString.empty)(_ ++ _)
          .map(_.utf8String)
      }
  }

  /** Send SMS
    */
  def sendSms(to: String, message: String): Future[String] = {
    val formData = FormData(
      "username" -> username,
      "to"       -> to,
      "message"  -> message,
      "from"     -> "SHORTCODE" // Your AT shortcode
    )

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = s"$baseUrl/messaging",
      headers = List(
        Authorization(BasicHttpCredentials(username, apiKey))
      ),
      entity = formData.toEntity
    )

    Http()
      .singleRequest(request)
      .flatMap { response =>
        response
          .entity
          .dataBytes
          .runFold(ByteString.empty)(_ ++ _)
          .map(_.utf8String)
      }
  }

}
