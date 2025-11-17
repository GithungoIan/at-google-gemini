ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.17"

val AkkaVersion       = "2.8.5"
val AkkaHttpVersion   = "10.5.3"
val PrometheusVersion = "0.16.0"

lazy val root = (project in file("."))
  .settings(
    name := "at-google-gemini",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"             %% "akka-actor-typed"         % AkkaVersion,
      "com.typesafe.akka"             %% "akka-stream"              % AkkaVersion,
      "com.typesafe.akka"             %% "akka-http"                % AkkaHttpVersion,
      "com.typesafe.akka"             %% "akka-http-spray-json"     % AkkaHttpVersion,
      "io.circe"                      %% "circe-core"               % "0.14.6",
      "io.circe"                      %% "circe-generic"            % "0.14.6",
      "io.circe"                      %% "circe-parser"             % "0.14.6",
      "de.heikoseeberger"             %% "akka-http-circe"          % "1.39.2",
      "com.softwaremill.sttp.client3" %% "core"                     % "3.9.2",
      "com.softwaremill.sttp.client3" %% "circe"                    % "3.9.2",
      "ch.qos.logback"                 % "logback-classic"          % "1.4.11",
      "io.prometheus"                  % "simpleclient"             % PrometheusVersion,
      "io.prometheus"                  % "simpleclient_hotspot"     % PrometheusVersion,
      "io.prometheus"                  % "simpleclient_common"      % PrometheusVersion,
      "com.typesafe.scala-logging"    %% "scala-logging"            % "3.9.5",
      "org.scalatest"                 %% "scalatest"                % "3.2.17" % Test,
      "com.typesafe.akka"             %% "akka-actor-testkit-typed" % "2.8.5"  % Test
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )
