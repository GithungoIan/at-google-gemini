package com.githungo.gemini

import java.time.Instant
import java.util.UUID

import spray.json._

package object domains {

  sealed trait CallState

  object CallState {
    case object Ringing extends CallState

    case object Active extends CallState

    case object Completed extends CallState

    case object Failed extends CallState
  }

  case class CallSession(
    callId: String,
    phoneNumber: String,
    state: CallState,
    messages: List[ConversationMessage],
    startedAt: Instant,
    endedAt: Option[Instant] = None,
    metadata: Map[String, String] = Map.empty
  ) {

    def addMessage(message: ConversationMessage): CallSession = copy(
      messages = messages :+ message
    )

    def getUserMessages: List[ConversationMessage] = messages.filter(
      _.role == MessageRole.User
    )

    def getAssistantMessages: List[ConversationMessage] = messages.filter(
      _.role == MessageRole.Assistant
    )

  }

  sealed trait MessageRole

  object MessageRole {
    case object User extends MessageRole

    case object Assistant extends MessageRole

    case object System extends MessageRole
  }

  case class ConversationMessage(
    id: UUID,
    role: MessageRole,
    content: String,
    audioUrl: Option[String] = None,
    timestamp: Instant = Instant.now()
  )

  case class VoiceEventPayload(
    sessionId: String,
    callSessionState: String,
    direction: String,
    callerNumber: String,
    dtmfDigits: Option[String],
    recordingUrl: Option[String],
    durationInSeconds: Option[Int]
  )

  sealed trait VoiceResponse {
    def toXml: String
  }

  case class Say(text: String, voice: String = "woman") extends VoiceResponse {

    def toXml: String =
      s"""<Response>
         |  <Say voice="$voice">$text</Say>
         |</Response>""".stripMargin

  }

  case class SayAndRecord(
    text: String,
    maxLength: Int = 30,
    timeout: Int = 5,
    playBeep: Boolean = true
  ) extends VoiceResponse {

    def toXml: String =
      s"""<Response>
         |  <Say>$text</Say>
         |  <Record maxLength="$maxLength" timeout="$timeout" playBeep="$playBeep"/>
         |</Response>""".stripMargin

  }

  case class Hangup(message: String) extends VoiceResponse {

    def toXml: String =
      s"""<Response>
         |  <Say>$message</Say>
         |  <Hangup/>
         |</Response>""".stripMargin

  }

  case class TransferCall(
    phoneNumber: String,
    message: String = "Please hold while I transfer you"
  ) extends VoiceResponse {

    def toXml: String =
      s"""<Response>
         |  <Say>$message</Say>
         |  <Dial phoneNumbers="$phoneNumber"/>
         |</Response>""".stripMargin

  }

  trait JsonSupport extends DefaultJsonProtocol {

    implicit object CallStateFormat extends JsonFormat[CallState] {

      def write(state: CallState): JsValue = JsString(state match {
        case CallState.Ringing   => "ringing"
        case CallState.Active    => "active"
        case CallState.Completed => "completed"
        case CallState.Failed    => "failed"
      })

      def read(value: JsValue): CallState =
        value match {
          case JsString("ringing")   => CallState.Ringing
          case JsString("active")    => CallState.Active
          case JsString("completed") => CallState.Completed
          case JsString("failed")    => CallState.Failed
          case _                     => deserializationError(s"Invalid CallState: $value")
        }

    }

    implicit object MessageRoleFormat extends JsonFormat[MessageRole] {

      def write(role: MessageRole): JsValue = JsString(role match {
        case MessageRole.User      => "user"
        case MessageRole.Assistant => "assistant"
        case MessageRole.System    => "system"
      })

      def read(value: JsValue): MessageRole =
        value match {
          case JsString("user")      => MessageRole.User
          case JsString("assistant") => MessageRole.Assistant
          case JsString("system")    => MessageRole.System
          case _ => deserializationError(s"Invalid MessageRole: $value")
        }

    }

    implicit val uuidFormat: JsonFormat[UUID] =
      new JsonFormat[UUID] {
        def write(x: UUID) = JsString(x.toString)

        def read(v: JsValue): UUID =
          v match {
            case JsString(str) => UUID.fromString(str)
            case _             => deserializationError("Expected UUID string")
          }

      }

    implicit val instantFormat: JsonFormat[Instant] =
      new JsonFormat[Instant] {
        def write(i: Instant) = JsString(i.toString)

        def read(v: JsValue): Instant =
          v match {
            case JsString(str) => Instant.parse(str)
            case _             => deserializationError("Expected Instant string")
          }

      }

    implicit val conversationMessageFormat: RootJsonFormat[ConversationMessage] =
      jsonFormat5(ConversationMessage)

    implicit val callSessionFormat: RootJsonFormat[CallSession] = jsonFormat7(CallSession)

    implicit val voiceEventPayloadFormat: RootJsonFormat[VoiceEventPayload] = jsonFormat7(
      VoiceEventPayload
    )

    implicit object VoiceResponseFormat extends RootJsonFormat[VoiceResponse] {

      def write(resp: VoiceResponse): JsValue =
        resp match {
          case Say(text, voice) =>
            JsObject(
              "type"  -> JsString("Say"),
              "text"  -> JsString(text),
              "voice" -> JsString(voice)
            )

          case SayAndRecord(text, maxLength, timeout, playBeep) =>
            JsObject(
              "type"      -> JsString("SayAndRecord"),
              "text"      -> JsString(text),
              "maxLength" -> JsNumber(maxLength),
              "timeout"   -> JsNumber(timeout),
              "playBeep"  -> JsBoolean(playBeep)
            )

          case Hangup(message) =>
            JsObject(
              "type"    -> JsString("Hangup"),
              "message" -> JsString(message)
            )

          case TransferCall(phoneNumber, message) =>
            JsObject(
              "type"        -> JsString("TransferCall"),
              "phoneNumber" -> JsString(phoneNumber),
              "message"     -> JsString(message)
            )
        }

      def read(json: JsValue): VoiceResponse =
        json.asJsObject.getFields("type") match {
          case Seq(JsString("Say")) => json.convertTo[Say]

          case Seq(JsString("SayAndRecord")) => json.convertTo[SayAndRecord]

          case Seq(JsString("Hangup")) => json.convertTo[Hangup]

          case Seq(JsString("TransferCall")) => json.convertTo[TransferCall]

          case other => deserializationError(s"Unknown VoiceResponse type: $other")
        }

    }

    implicit val sayFormat: RootJsonFormat[Say] = jsonFormat2(Say)

    implicit val sayAndRecordFormat: RootJsonFormat[SayAndRecord] = jsonFormat4(
      SayAndRecord
    )

    implicit val hangupFormat: RootJsonFormat[Hangup] = jsonFormat1(Hangup)

    implicit val transferCallFormat: RootJsonFormat[TransferCall] = jsonFormat2(
      TransferCall
    )

  }

}
