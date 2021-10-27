package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{FileManager, OnboardingFilePath}

import java.io.{ByteArrayOutputStream, File, FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final class FileManagerImpl extends FileManager {

  val currentPath: Path = Paths.get(System.getProperty("user.dir"))

  override def store(tokenId: UUID, fileParts: (FileInfo, File)): Future[OnboardingFilePath] =
    Future.fromTry {
      Try {
        val destPath = createPath(tokenId.toString, fileParts._1.getContentType.toString(), fileParts._1.getFileName)

        moveRenameFile(fileParts._2.getPath, destPath).toString
      }
    }

  def get(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      val inputStream: InputStream            = new FileInputStream(filePath)
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val _                                   = inputStream.transferTo(outputStream)
      outputStream
    }
  }

  override def delete(filePath: String): Future[Boolean] = Future.fromTry {
    Try {
      val file: File = Paths.get(filePath).toFile
      file.delete()
    }
  }

  private def createPath(tokenId: String, contentType: String, fileName: String): String = {

    val docsPath: Path =
      Paths.get(currentPath.toString, s"target/pdnd-interop/docs/$tokenId/${contentType}")
    val pathCreated: Path = Files.createDirectories(docsPath)

    Paths.get(pathCreated.toString, s"${fileName}").toString

  }

  private def moveRenameFile(source: String, destination: String): Path = {
    Files.move(Paths.get(source), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING)

  }

}
