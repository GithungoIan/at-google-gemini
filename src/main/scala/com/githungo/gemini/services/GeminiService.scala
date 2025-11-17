package com.githungo.gemini.services

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString

import io.circe.Json
import io.circe.parser._

import com.githungo._
import gemini.domains._

class GeminiService(apiKey: String)(
  implicit system: ActorSystem,
  ec: ExecutionContext
) {

  private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

  def transcribeAudio(audioUrl: String): Future[String] =
    for {
      audioBytes <- downloadAudio(audioUrl)
      transcription <- transcribeWithGemini(audioBytes)
    } yield transcription

  private def downloadAudio(url: String): Future[ByteString] = Http()
    .singleRequest(HttpRequest(uri = url))
    .flatMap { response =>
      if (response.status.isSuccess()) {
        response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
      } else {
        Future.failed(
          new Exception(s"Failed to download audio: ${response.status}")
        )
      }
    }

  private def transcribeWithGemini(audioBytes: ByteString): Future[String] = {
    // Gemini audio transcription
    val audioBase64 = java.util.Base64.getEncoder.encodeToString(audioBytes.toArray)

    val requestBody = Json.obj(
      "contents" -> Json.arr(
        Json.obj(
          "parts" -> Json.arr(
            Json.obj(
              "inline_data" -> Json.obj(
                "mime_type" -> Json.fromString("audio/wav"),
                "data" -> Json.fromString(audioBase64)
              )
            ),
            Json.obj(
              "text" -> Json.fromString("Transcribe this audio")
            )
          )
        )
      )
    ).noSpaces

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = s"$baseUrl/models/gemini-pro:generateContent?key=$apiKey",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        requestBody
      )
    )

    Http()
      .singleRequest(request)
      .flatMap { response =>
        Unmarshal(response.entity).to[String].map { body =>
          parse(body) match {
            case Right(json) =>
              json
                .hcursor
                .downField("candidates")
                .downArray
                .downField("content")
                .downField("parts")
                .downArray
                .downField("text")
                .as[String]
                .getOrElse("Failed to parse transcription")
            case Left(error) =>
              throw new Exception(s"Failed to parse Gemini response: $error")
          }
        }
      }
  }

  def generateResponse(
    messages: List[ConversationMessage]
  ): Future[String] = {
    val geminiMessages = messages.map { msg =>
      Json.obj(
        "role" -> Json.fromString(
          if (msg.role == MessageRole.User) "user" else "model"
        ),
        "parts" -> Json.arr(
          Json.obj("text" -> Json.fromString(msg.content))
        )
      )
    }

    val requestBody = Json.obj(
      "contents" -> Json.arr(geminiMessages: _*),
      "generationConfig" -> Json.obj(
        "temperature" -> Json.fromDoubleOrNull(0.7),
        "maxOutputTokens" -> Json.fromInt(150), // Keep responses concise for voice
        "topP" -> Json.fromDoubleOrNull(0.8),
        "topK" -> Json.fromInt(40)
      )
    ).noSpaces

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = s"$baseUrl/models/gemini-pro:generateContent?key=$apiKey",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        requestBody
      )
    )

    Http()
      .singleRequest(request)
      .flatMap { response =>
        Unmarshal(response.entity).to[String].map { body =>
          parse(body) match {
            case Right(json) =>
              json
                .hcursor
                .downField("candidates")
                .downArray
                .downField("content")
                .downField("parts")
                .downArray
                .downField("text")
                .as[String]
                .getOrElse("I apologize, I couldn't generate a response.")
            case Left(error) =>
              throw new Exception(s"Failed to parse Gemini response: $error")
          }
        }
      }
  }

}
