package com.githungo.gemini

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import com.githungo._
import gemini.actors.{CallSupervisor, SMSSupervisor}
import gemini.api.ApiVersioning
import gemini.services.{AfricasTalkingService, GeminiService}

object Main extends App {

  // Load configuration from environment
  private val atApiKey = sys.env.getOrElse(
    "AT_API_KEY",
    throw new IllegalArgumentException("AT_API_KEY environment variable not set")
  )
  private val atUsername = sys.env.getOrElse(
    "AT_USERNAME",
    throw new IllegalArgumentException(
      "AT_USERNAME environment variable not set"
    )
  )
  private val geminiApiKey = sys.env.getOrElse(
    "GEMINI_API_KEY",
    throw new IllegalArgumentException(
      "GEMINI_API_KEY environment variable not set"
    )
  )
  private val port = sys.env.getOrElse("PORT", "8080").toInt
  private val interface = sys.env.getOrElse("INTERFACE", "0.0.0.0")

  implicit val system: ActorSystem[CallSupervisor.Command] =
    ActorSystem(rootBehavior(), "at-google-gemini")
  implicit val executionContext: ExecutionContextExecutor =
    system.executionContext

  private def rootBehavior() = Behaviors.setup[CallSupervisor.Command] {
    context =>
      implicit val classicSystem: akka.actor.ActorSystem =
        context.system.classicSystem

      val geminiService = new GeminiService(geminiApiKey)(
        classicSystem,
        executionContext
      )
      val atService = new AfricasTalkingService(atApiKey, atUsername)(
        classicSystem,
        executionContext
      )

      val callSupervisor = context.spawn(
        CallSupervisor(geminiService, atService),
        "call-supervisor"
      )

      val smsSupervisor = context.spawn(
        SMSSupervisor(geminiService, atService),
        "sms-supervisor"
      )

      val apiVersioning = new ApiVersioning(callSupervisor, smsSupervisor)(
        context.system
      )

      val routes: Route = apiVersioning.routes

      // Start HTTP server
      val bindingFuture = Http()(classicSystem)
        .newServerAt(interface, port)
        .bindFlow(Route.toFlow(routes)(classicSystem))

      bindingFuture.onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          context.log.info(
            s"VoiceAI service started at http://${address.getHostString}:${address.getPort}/"
          )
          context.log.info(s"Environment: ${sys.env.getOrElse("ENVIRONMENT", "development")}")
          context.log.info(s"")
          context.log.info(s"API Version 1 Endpoints:")
          context.log.info(s"  Voice Webhook: http://${address.getHostString}:${address.getPort}/api/v1/webhooks/voice/events")
          context.log.info(s"  SMS Webhook: http://${address.getHostString}:${address.getPort}/api/v1/webhooks/sms/incoming")
          context.log.info(s"")
          context.log.info(s"Legacy Endpoints (deprecated):")
          context.log.info(s"  Voice: http://${address.getHostString}:${address.getPort}/webhooks/voice/events")
          context.log.info(s"  SMS: http://${address.getHostString}:${address.getPort}/webhooks/sms/incoming")
          context.log.info(s"")
          context.log.info(s"Infrastructure:")
          context.log.info(s"  Health: http://${address.getHostString}:${address.getPort}/admin/health")
          context.log.info(s"  Metrics: http://${address.getHostString}:${address.getPort}/metrics")

        case Failure(ex) =>
          context.log.error(s"Failed to bind HTTP server: ${ex.getMessage}")
          system.terminate()
      }

      // Register shutdown hook
      sys.addShutdownHook {
        context.log.info("Shutting down VoiceAI service...")
        bindingFuture
          .flatMap(_.unbind())
          .onComplete(_ => system.terminate())
      }

      // Return behavior
      Behaviors.receiveMessage { msg =>
        callSupervisor ! msg
        Behaviors.same
      }
  }

  println(
    """
      |╔══════════════════════════════════════════════════════════════╗
      |║                                                              ║
      |║            Africa's Talking + Gemini                         ║
      |║                                                              ║
      |║  Building intelligent voice and SMS systems with AI          ║
      |║                                                              ║
      |╚══════════════════════════════════════════════════════════════╝
      |
      |Starting server...
      |""".stripMargin
  )

}
