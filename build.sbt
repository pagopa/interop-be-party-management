import sbt.Credentials
import sbt.Keys.credentials

import scala.sys.process.Process

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / organization := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / libraryDependencies := Dependencies.Jars.`server`.map(m =>
  if (scalaVersion.value.startsWith("3.0"))
    m.withDottyCompat(scalaVersion.value)
  else
    m
)
ThisBuild / version := {
  Process("./version.sh").lineStream_!.head.replaceFirst("v", "")
}

lazy val generateCode = taskKey[Unit]("A task for generating the code starting from the swagger definition")

generateCode := {
  import sys.process._

  val packagePrefix = name.value
    .replaceFirst("pdnd-", "pdnd.")
    .replaceFirst("interop-", "interop.")
    .replaceFirst("uservice-", "uservice.")
    .replaceAll("-", "")

  Process(s"""openapi-generator-cli generate -t template/scala-akka-http-server
             |                               -i src/main/resources/interface-specification.yml
             |                               -g scala-akka-http-server
             |                               -p projectName=${name.value}
             |                               -p invokerPackage=it.pagopa.${packagePrefix}.server
             |                               -p modelPackage=it.pagopa.${packagePrefix}.model
             |                               -p apiPackage=it.pagopa.${packagePrefix}.api
             |                               -p dateLibrary=java8
             |                               -o generated""".stripMargin).!!

  Process(s"""openapi-generator-cli generate -t template/scala-akka-http-client
             |                               -i src/main/resources/interface-specification.yml
             |                               -g scala-akka
             |                               -p projectName=${name.value}
             |                               -p invokerPackage=it.pagopa.${packagePrefix}.client.invoker
             |                               -p modelPackage=it.pagopa.${packagePrefix}.client.model
             |                               -p apiPackage=it.pagopa.${packagePrefix}.client.api
             |                               -p dateLibrary=java8
             |                               -o client""".stripMargin).!!

}

(compile in Compile) := ((compile in Compile) dependsOn generateCode).value

cleanFiles += baseDirectory.value / "generated" / "src"

cleanFiles += baseDirectory.value / "client" / "src"

lazy val generated = project.in(file("generated")).settings(scalacOptions := Seq(), scalafmtOnCompile := true)

lazy val nexusHost = Option(System.getenv("NEXUS_HOST")).getOrElse("my.artifact.repo.net")
lazy val nexusUser = Option(System.getenv("NEXUS_USER")).getOrElse("user")
lazy val nexusPass = Option(System.getenv("NEXUS_PASSWORD")).getOrElse("password")

lazy val printer = taskKey[Unit]("Printer")
printer := println("!!!!!!!!!!!")
printer :=  println(Option(System.getenv("NEXUS_HOST")))
printer :=  println(nexusHost)
printer :=  println("!!!!!!!!!!!")

lazy val client = project
  .in(file("client"))
  .settings(
    name := "pdnd-interop-uservice-party-management",
    scalacOptions := Seq(),
    scalafmtOnCompile := true,
    libraryDependencies := Dependencies.Jars.client.map(m =>
      if (scalaVersion.value.startsWith("3.0"))
        m.withDottyCompat(scalaVersion.value)
      else
        m
    ),
    credentials += Credentials("Sonatype Nexus Repository Manager", nexusHost, nexusUser, nexusPass),
    updateOptions := updateOptions.value.withGigahorse(false),
    publishTo := {
      val nexus = s"$nexusHost/nexus/repository/"

      if (isSnapshot.value)
        Some("snapshots" at nexus + "maven-snapshots/")
      else
        Some("releases" at nexus + "maven-releases/")
    }
  )

lazy val root = (project in file("."))
  .settings(
    name := "pdnd-interop-uservice-party-management",
    parallelExecution in Test := false,
    dockerBuildOptions ++= Seq("--network=host"),
    dockerRepository in Docker := Some(System.getenv("DOCKER_REPO")),
    version in Docker := s"${
      val buildVersion = (version in ThisBuild).value
      if (buildVersion == "latest")
        buildVersion
      else
        s"v$buildVersion"
    }".toLowerCase,
    packageName in Docker := s"services/${name.value}",
    daemonUser in Docker := "daemon",
    dockerExposedPorts in Docker := Seq(8080),
    dockerBaseImage in Docker := "openjdk:8-jre-alpine",
    dockerUpdateLatest in Docker := true,
    wartremoverErrors ++= Warts.unsafe,
    scalafmtOnCompile := true
  )
  .aggregate(client)
  .dependsOn(generated)
  .enablePlugins(AshScriptPlugin, DockerPlugin)
