import Versions._
import sbt._

object Dependencies {

  private[this] object akka {
    lazy val namespace     = "com.typesafe.akka"
    lazy val actorTyped    = namespace                       %% "akka-actor-typed"           % akkaVersion
    lazy val actor         = namespace                       %% "akka-actor"                 % akkaVersion
    lazy val persistence   = namespace                       %% "akka-persistence-typed"     % akkaVersion
    lazy val serialization = namespace                       %% "akka-serialization-jackson" % akkaVersion
    lazy val stream        = namespace                       %% "akka-stream"                % akkaVersion
    lazy val http          = namespace                       %% "akka-http"                  % akkaHttpVersion
    lazy val httpJson      = namespace                       %% "akka-http-spray-json"       % akkaHttpVersion
    lazy val httpJson4s    = "de.heikoseeberger"             %% "akka-http-json4s"           % "1.36.0"
    lazy val management    = "com.lightbend.akka.management" %% "akka-management"            % "1.0.10"
    lazy val slf4j         = namespace                       %% "akka-slf4j"                 % akkaVersion

  }

  private[this] object json4s {
    lazy val namespace = "org.json4s"
    lazy val jackson   = namespace %% "json4s-jackson" % "3.6.11"
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object kamon {
    lazy val namespace  = "io.kamon"
    lazy val bundle     = namespace %% "kamon-bundle"     % kamonVersion
    lazy val prometheus = namespace %% "kamon-prometheus" % kamonVersion
  }

  private[this] object scalatest {
    lazy val namespace = "org.scalatest"
    lazy val core      = namespace %% "scalatest" % scalatestVersion
  }

  private[this] object scalamock {
    lazy val namespace = "org.scalamock"
    lazy val core      = namespace %% "scalamock" % scalaMockVersion
  }

  object Jars {
    lazy val `server`: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"    % "javax.annotation-api" % "1.3.2" % "compile",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      //
      akka.actorTyped    % Compile,
      akka.actor         % Compile,
      akka.persistence   % Compile,
      akka.serialization % Compile,
      akka.stream        % Compile,
      akka.http          % Compile,
      akka.httpJson      % Compile,
      akka.management    % Compile,
      logback.classic    % Compile,
      akka.slf4j         % Compile,
      kamon.bundle       % Compile,
      kamon.prometheus   % Compile,
      scalatest.core     % Test,
      scalamock.core     % Test
    )
    lazy val client: Seq[ModuleID] =
      Seq(akka.stream % Compile, akka.http % Compile, akka.httpJson4s % Compile, json4s.jackson % Compile)
  }
}
