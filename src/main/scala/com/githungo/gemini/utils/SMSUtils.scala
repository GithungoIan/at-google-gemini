package com.githungo.gemini.utils

import scala.concurrent.{ExecutionContext, Future}

import com.githungo.gemini.services.AfricasTalkingService

object SMSUtils {

  /**
    * Split a message into chunks that fit within SMS character limits
    * @param text The text to split
    * @param maxLength Maximum length per message (default 160 for standard SMS)
    * @return List of message parts
    */
  def splitMessage(text: String, maxLength: Int = 160): List[String] = {
    if (text.length <= maxLength) {
      List(text)
    } else {
      // Split by words to avoid breaking in the middle of words
      val words = text.split(" ")
      words
        .foldLeft(List.empty[String]) { (acc, word) =>
          acc match {
            case Nil => List(word)
            case head :: tail =>
              val candidate = s"$head $word"
              if (candidate.length <= maxLength) {
                candidate :: tail
              } else {
                word :: head :: tail
              }
          }
        }
        .reverse
    }
  }

  /**
    * Send a potentially long SMS message, splitting it into parts if needed
    * @param phoneNumber Recipient phone number
    * @param message Message to send
    * @param atService Africa's Talking service instance
    * @param ec Execution context
    * @return Future with list of send results
    */
  def sendLongSMS(
    phoneNumber: String,
    message: String,
    atService: AfricasTalkingService
  )(implicit ec: ExecutionContext): Future[List[String]] = {
    val parts = splitMessage(message)

    Future.sequence(
      parts.zipWithIndex.map { case (part, idx) =>
        val formattedPart = if (parts.length > 1) {
          s"(${idx + 1}/${parts.length}) $part"
        } else {
          part
        }

        atService.sendSms(phoneNumber, formattedPart)
      }
    )
  }

  /**
    * Sanitize user input for processing
    * @param input Raw user input
    * @return Sanitized input
    */
  def sanitizeInput(input: String): String = {
    input
      .trim
      .replaceAll("""<script[^>]*>.*?</script>""", "") // Remove script tags
      .replaceAll("""[^\w\s\?\.\,\!\'\-]""", "") // Keep only alphanumeric and common punctuation
      .take(1000) // Limit length
  }

  /**
    * Validate phone number format
    * @param phoneNumber Phone number to validate
    * @return true if valid, false otherwise
    */
  def isValidPhoneNumber(phoneNumber: String): Boolean = {
    val pattern = """^\+?[1-9]\d{1,14}$""".r
    pattern.findFirstIn(phoneNumber).isDefined
  }

  /**
    * Format a phone number to E.164 format
    * @param phoneNumber Phone number to format
    * @param defaultCountryCode Default country code (e.g., "254" for Kenya)
    * @return Formatted phone number
    */
  def formatPhoneNumber(phoneNumber: String, defaultCountryCode: String = "254"): String = {
    val cleaned = phoneNumber.replaceAll("[^0-9+]", "")

    if (cleaned.startsWith("+")) {
      cleaned
    } else if (cleaned.startsWith("0")) {
      s"+$defaultCountryCode${cleaned.substring(1)}"
    } else {
      s"+$defaultCountryCode$cleaned"
    }
  }

}
