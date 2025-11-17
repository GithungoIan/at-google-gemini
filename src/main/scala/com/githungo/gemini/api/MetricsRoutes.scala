package com.githungo.gemini.api

import java.io.StringWriter

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

class MetricsRoutes {

  val routes: Route = path("metrics") {
    get {
      val writer = new StringWriter()
      TextFormat.write004(
        writer,
        CollectorRegistry.defaultRegistry.metricFamilySamples()
      )

      complete(
        HttpEntity(
          ContentTypes.`text/plain(UTF-8)`,
          writer.toString
        )
      )
    }
  }

}
