package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import com.azure.storage.blob.specialized.BlockBlobClient
import com.azure.storage.blob.{BlobClient, BlobServiceClient, BlobServiceClientBuilder}
import com.azure.storage.common.StorageSharedKeyCredential
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApplicationConfiguration.storageAccountInfo
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{FileManager, OnboardingFilePath}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

//using both final and protected to overcome the possible "never used" compile error for a private constructor
final class BlobStorageManagerImpl protected extends FileManager {

  lazy val azureBlobClient = {
    val accountName: String = storageAccountInfo.applicationId
    val accountKey: String  = storageAccountInfo.applicationSecret
    val endpoint: String    = storageAccountInfo.endpoint
    val credential          = new StorageSharedKeyCredential(accountName, accountKey)
    val storageClient: BlobServiceClient =
      new BlobServiceClientBuilder().endpoint(endpoint).credential(credential).buildClient

    storageClient
  }

  override def store(id: UUID, fileParts: (FileInfo, File)): Future[OnboardingFilePath] = Future.fromTry {
    Try {
      val blobKey = createBlobKey(
        id.toString,
        contentType = fileParts._1.getContentType.toString(),
        fileName = fileParts._1.getFileName
      )

      val blobContainerClient    = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
      val blobClient: BlobClient = blobContainerClient.getBlobClient(blobKey)
      blobClient.uploadFromFile(fileParts._2.getPath)

      blobKey
    }
  }

  private def createBlobKey(tokenId: String, contentType: String, fileName: String): String =
    s"parties/docs/$tokenId/${contentType}/$fileName"

  override def get(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      val blobContainerClient         = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
      val blobClient: BlockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient

      val dataSize: Int                       = blobClient.getProperties.getBlobSize.toInt
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream(dataSize)
      val _                                   = blobClient.downloadStream(outputStream)
      outputStream
    }
  }

  override def delete(filePath: String): Future[Boolean] = {
    Try {
      val blobContainerClient         = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
      val blobClient: BlockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
      blobClient.delete
    }.fold(error => Future.failed[Boolean](error), _ => Future.successful(true))
  }

}
