package com.githungo.gemini.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import spray.json._

import com.githungo._
import gemini.domains.JsonSupport

class AdminRoutes(
  implicit system: ActorSystem[_]
) extends SprayJsonSupport
  with JsonSupport {

  case class HealthStatus(
    status: String,
    timestamp: String,
    version: String,
    checks: Map[String, String]
  )

  case class SystemMetrics(
    activeCallSessions: Int,
    activeSMSSessions: Int,
    totalCallsToday: Long,
    totalSMSToday: Long,
    uptime: String
  )

  implicit val healthStatusFormat: RootJsonFormat[HealthStatus] = jsonFormat4(
    HealthStatus
  )

  implicit val systemMetricsFormat: RootJsonFormat[SystemMetrics] = jsonFormat5(
    SystemMetrics
  )

  val routes: Route =
    pathPrefix("admin") {
      path("health") {
        get {
          // Liveness probe
          complete(StatusCodes.OK, "OK")
        }
      } ~
        path("health" / "ready") {
          get {
            // Readiness probe
            val checks = performHealthChecks()

            if (checks.values.forall(_ == "healthy")) {
              complete(
                StatusCodes.OK,
                HealthStatus(
                  status    = "healthy",
                  timestamp = java.time.Instant.now().toString,
                  version   = "1.0.0",
                  checks    = checks
                )
              )
            } else {
              complete(
                StatusCodes.ServiceUnavailable,
                HealthStatus(
                  status    = "unhealthy",
                  timestamp = java.time.Instant.now().toString,
                  version   = "1.0.0",
                  checks    = checks
                )
              )
            }
          }
        } ~
        path("metrics" / "system") {
          get {
            // System metrics for admin dashboard
            val metrics = getSystemMetrics
            complete(StatusCodes.OK, metrics)
          }
        } ~
        path("version") {
          get {
            complete(
              StatusCodes.OK,
              Map("version" -> "1.0.0", "build" -> "latest")
            )
          }
        }
    }

  private def performHealthChecks(): Map[String, String] = Map(
    "gemini_api"      -> checkGeminiAPI(),
    "africas_talking" -> checkAfricasTalkingAPI(),
    "actors"          -> "healthy"
  )

  private def checkGeminiAPI(): String =
    if (sys.env.contains("GEMINI_API_KEY"))
      "healthy"
    else
      "unhealthy"

  private def checkAfricasTalkingAPI(): String =
    if (sys.env.contains("AT_API_KEY"))
      "healthy"
    else
      "unhealthy"

  private def getSystemMetrics: SystemMetrics = {
    val uptimeMs = sys.runtime.totalMemory()
    val uptimeSeconds =
      java
        .lang
        .management
        .ManagementFactory
        .getRuntimeMXBean
        .getUptime / 1000

    SystemMetrics(
      activeCallSessions = 0, // TODO: Get from CallSupervisor
      activeSMSSessions  = 0, // TODO: Get from SMSSupervisor
      totalCallsToday    = 0, // TODO: Get from metrics
      totalSMSToday      = 0, // TODO: Get from metrics
      uptime             = s"${uptimeSeconds}s"
    )
  }

}
