package com.githungo.gemini.logging

import org.slf4j.LoggerFactory

import spray.json._
import DefaultJsonProtocol._

object StructuredLogger {

  private val logger = LoggerFactory.getLogger("at-google-gemini")

  case class LogEntry(
    timestamp: String,
    level: String,
    service: String,
    event: String,
    phoneNumber: Option[String] = None,
    callId: Option[String] = None,
    duration: Option[Long] = None,
    error: Option[String] = None,
    metadata: Map[String, String] = Map.empty
  )

  implicit val logEntryFormat: RootJsonFormat[LogEntry] = jsonFormat9(LogEntry)

  def info(event: String, metadata: Map[String, String] = Map.empty): Unit = {
    val entry = LogEntry(
      timestamp = java.time.Instant.now().toString,
      level = "INFO",
      service = "at-google-gemini",
      event = event,
      metadata = metadata
    )
    logger.info(entry.toJson.compactPrint)
  }

  def warn(
    event: String,
    metadata: Map[String, String] = Map.empty
  ): Unit = {
    val entry = LogEntry(
      timestamp = java.time.Instant.now().toString,
      level = "WARN",
      service = "at-google-gemini",
      event = event,
      metadata = metadata
    )
    logger.warn(entry.toJson.compactPrint)
  }

  def error(
    event: String,
    error: Throwable,
    metadata: Map[String, String] = Map.empty
  ): Unit = {
    val entry = LogEntry(
      timestamp = java.time.Instant.now().toString,
      level = "ERROR",
      service = "at-google-gemini",
      event = event,
      error = Some(error.getMessage),
      metadata = metadata
    )
    logger.error(entry.toJson.compactPrint, error)
  }

  def callStarted(callId: String, phoneNumber: String): Unit = {
    info(
      "call_started",
      Map("call_id" -> callId, "phone_number" -> phoneNumber)
    )
  }

  def callCompleted(callId: String, duration: Long): Unit = {
    info(
      "call_completed",
      Map("call_id" -> callId, "duration_ms" -> duration.toString)
    )
  }

  def smsReceived(phoneNumber: String, text: String): Unit = {
    info(
      "sms_received",
      Map("phone_number" -> phoneNumber, "message_length" -> text.length.toString)
    )
  }

  def smsSent(phoneNumber: String, success: Boolean): Unit = {
    info(
      "sms_sent",
      Map("phone_number" -> phoneNumber, "success" -> success.toString)
    )
  }

  def geminiApiCall(operation: String, duration: Long, success: Boolean): Unit = {
    info(
      "gemini_api_call",
      Map(
        "operation" -> operation,
        "duration_ms" -> duration.toString,
        "success" -> success.toString
      )
    )
  }

}
