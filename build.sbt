import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / organization := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / libraryDependencies := Dependencies.Jars.`server`.map(m =>
  if (scalaVersion.value.startsWith("3.0"))
    m.withDottyCompat(scalaVersion.value)
  else
    m
)

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

ThisBuild / version := "0.1.0-SNAPSHOT"

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

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

lazy val generated = project.in(file("generated")).settings(scalacOptions := Seq(), scalafmtOnCompile := true)

lazy val client = project
  .in(file("client"))
  .settings(
    name := "pdnd-interop-uservice-party-management-client",
    scalacOptions := Seq(),
    scalafmtOnCompile := true,
    version := (version in ThisBuild).value,
    libraryDependencies := Dependencies.Jars.client.map(m =>
      if (scalaVersion.value.startsWith("3.0"))
        m.withDottyCompat(scalaVersion.value)
      else
        m
    ),
    updateOptions := updateOptions.value.withGigahorse(false),
    publishTo := {
      val nexus = s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/"

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
    version in Docker := (version in ThisBuild).value,
    packageName in Docker := s"services/${name.value}",
    daemonUser in Docker := "daemon",
    dockerExposedPorts in Docker := Seq(8080),
    dockerBaseImage in Docker := "adoptopenjdk:11-jdk-hotspot",
    dockerUpdateLatest in Docker := true,
    Compile / compile / wartremoverErrors ++= Warts.unsafe,
    wartremoverExcluded += sourceManaged.value,
    scalafmtOnCompile := true
  )
  .aggregate(client)
  .dependsOn(generated)
  .enablePlugins(AshScriptPlugin, DockerPlugin)
