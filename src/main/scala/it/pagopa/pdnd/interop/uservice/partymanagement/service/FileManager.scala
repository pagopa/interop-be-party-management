package it.pagopa.pdnd.interop.uservice.partymanagement.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.partymanagement.service.impl.{
  BlobStorageManagerImpl,
  FileManagerImpl,
  S3ManagerImpl
}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Try}

trait FileManager {

  def store(tokenId: UUID, fileParts: (FileInfo, File)): Future[OnboardingFilePath]

  def get(filePath: String): Future[ByteArrayOutputStream]

  def delete(filePath: String): Future[Boolean]

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.AsInstanceOf"
  )
)
object FileManager {
  def getConcreteImplementation(fileManager: String): Try[FileManager] = {
    fileManager match {
      case "File"           => Try { new FileManagerImpl() }
      case "BlobStorage"    => Try { new BlobStorageManagerImpl() }
      case "S3"             => Try { new S3ManagerImpl() }
      case wrongManager @ _ => Failure(new RuntimeException(s"Unsupported file manager: $wrongManager"))
    }
  }
}
