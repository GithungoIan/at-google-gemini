package com.githungo.gemini.metrics

import io.prometheus.client._
import io.prometheus.client.hotspot.DefaultExports

object Metrics {

  // Counter: Total calls received
  val callsTotal: Counter = Counter
    .build()
    .name("voiceai_calls_total")
    .help("Total number of calls received")
    .labelNames("status")
    .register()

  // Histogram: Call duration distribution
  val callDuration: Histogram = Histogram
    .build()
    .name("voiceai_call_duration_seconds")
    .help("Call duration in seconds")
    .buckets(5, 10, 30, 60, 120, 300)
    .register()

  // Gauge: Active calls
  val activeCalls: Gauge = Gauge
    .build()
    .name("voiceai_active_calls")
    .help("Number of active calls")
    .register()

  // Counter: Gemini API calls
  val geminiApiCalls: Counter = Counter
    .build()
    .name("voiceai_gemini_api_calls_total")
    .help("Total Gemini API calls")
    .labelNames("operation", "status")
    .register()

  // Histogram: Gemini API latency
  val geminiApiLatency: Histogram = Histogram
    .build()
    .name("voiceai_gemini_api_latency_seconds")
    .help("Gemini API call latency")
    .labelNames("operation")
    .buckets(0.1, 0.5, 1.0, 2.0, 5.0)
    .register()

  // Counter: SMS sent
  val smsSent: Counter = Counter
    .build()
    .name("voiceai_sms_sent_total")
    .help("Total SMS messages sent")
    .labelNames("status")
    .register()

  // Counter: SMS received
  val smsReceived: Counter = Counter
    .build()
    .name("voiceai_sms_received_total")
    .help("Total SMS messages received")
    .register()

  // Histogram: SMS processing time
  val smsProcessingTime: Histogram = Histogram
    .build()
    .name("voiceai_sms_processing_seconds")
    .help("SMS processing time in seconds")
    .buckets(0.1, 0.5, 1.0, 2.0, 5.0)
    .register()

  // Gauge: Active SMS sessions
  val activeSMSSessions: Gauge = Gauge
    .build()
    .name("voiceai_active_sms_sessions")
    .help("Number of active SMS sessions")
    .register()

  // JVM metrics
  DefaultExports.initialize()

  // Helper methods
  def recordCallStarted(): Unit = {
    callsTotal.labels("started").inc()
    activeCalls.inc()
  }

  def recordCallCompleted(durationSeconds: Double): Unit = {
    callsTotal.labels("completed").inc()
    activeCalls.dec()
    callDuration.observe(durationSeconds)
  }

  def recordCallFailed(): Unit = {
    callsTotal.labels("failed").inc()
    activeCalls.dec()
  }

  def recordSMSReceived(): Unit = {
    smsReceived.inc()
  }

  def recordSMSSent(success: Boolean): Unit = {
    val status = if (success) "success" else "failure"
    smsSent.labels(status).inc()
  }

  def recordGeminiCall(operation: String, success: Boolean, durationSeconds: Double): Unit = {
    val status = if (success) "success" else "failure"
    geminiApiCalls.labels(operation, status).inc()
    geminiApiLatency.labels(operation).observe(durationSeconds)
  }

}
