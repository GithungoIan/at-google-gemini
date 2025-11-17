package com.githungo.gemini.api

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}

import com.githungo._
import gemini.actors.{CallSupervisor, SMSSupervisor}

// API Versioning Router
//
// Implements URL-based versioning following best practices:
// - /api/v1/* - Current stable version
// - /api/v2/* - Future version (when needed)
//
// Legacy endpoints (without /api prefix) are maintained for backward compatibility
// with deprecation headers.
class ApiVersioning(
  callSupervisor: ActorRef[CallSupervisor.Command],
  smsSupervisor: ActorRef[SMSSupervisor.Command]
)(implicit system: ActorSystem[_]) {

  // Route instances
  private val webhookRoutesV1 = new WebhookRoutes(callSupervisor)
  private val smsRoutesV1 = new SMSRoutes(smsSupervisor)
  private val adminRoutes = new AdminRoutes()
  private val metricsRoutes = new MetricsRoutes()

  // Add deprecation headers to a route
  private def deprecated(
    version: String,
    sunsetDate: String,
    alternativeEndpoint: String
  ): Directive0 = {
    respondWithHeaders(
      RawHeader("Deprecation", "true"),
      RawHeader("Sunset", sunsetDate),
      RawHeader("Link", s"<$alternativeEndpoint>; rel=\"alternate\""),
      RawHeader(
        "X-API-Warn",
        s"API version $version is deprecated. Please migrate to $alternativeEndpoint by $sunsetDate"
      )
    )
  }

  // API Version 1 routes
  private val v1Routes: Route =
    pathPrefix("api" / "v1") {
      pathPrefix("webhooks") {
        webhookRoutesV1.routes ~ smsRoutesV1.routes
      }
    }

  // Future API Version 2 routes (placeholder for when breaking changes are needed)
  private val v2Routes: Route =
    pathPrefix("api" / "v2") {
      pathPrefix("webhooks") {
        // Future: v2 implementations with breaking changes
        complete(
          StatusCodes.NotImplemented,
          "API v2 is not yet available. Please use /api/v1/"
        )
      }
    }

  // Legacy routes (backward compatibility)
  // These maintain the old endpoint structure for existing integrations
  // but add deprecation headers to encourage migration
  private val legacyRoutes: Route =
    deprecated(
      version = "legacy",
      sunsetDate = "2026-06-01",
      alternativeEndpoint = "/api/v1/webhooks"
    ) {
      pathPrefix("webhooks") {
        webhookRoutesV1.routes ~ smsRoutesV1.routes
      }
    }

  // Admin and monitoring routes (non-versioned, stable infrastructure)
  private val infrastructureRoutes: Route =
    adminRoutes.routes ~ metricsRoutes.routes

  // Root information endpoint
  private val rootRoute: Route =
    pathEndOrSingleSlash {
      get {
        complete(
          StatusCodes.OK,
          s"""
             |{
             |  "service": "at-google-gemini",
             |  "version": "1.0.0",
             |  "status": "operational",
             |  "api_versions": {
             |    "current": "v1",
             |    "available": ["v1"],
             |    "deprecated": ["legacy"]
             |  },
             |  "endpoints": {
             |    "voice_webhooks": "/api/v1/webhooks/voice/events",
             |    "sms_webhooks": "/api/v1/webhooks/sms/incoming",
             |    "health": "/admin/health",
             |    "metrics": "/metrics"
             |  },
             |  "documentation": "https://github.com/your-repo/README.md"
             |}
             |""".stripMargin
        )
      }
    }

  // Combined routes with version precedence:
  // 1. Infrastructure routes (health, metrics)
  // 2. Versioned API routes (v1, v2)
  // 3. Legacy routes (deprecated)
  // 4. Root information
  val routes: Route =
    infrastructureRoutes ~
      v1Routes ~
      v2Routes ~
      legacyRoutes ~
      rootRoute

}
