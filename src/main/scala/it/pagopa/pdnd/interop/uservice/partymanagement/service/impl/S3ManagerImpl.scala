package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApplicationConfiguration.storageAccountInfo
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{FileManager, OnboardingFilePath}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.{RequestBody, ResponseTransformer}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}
import software.amazon.awssdk.services.s3.model._

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

//using both final and protected to overcome the possible "never used" compile error for a private constructor
final class S3ManagerImpl protected extends FileManager {

  lazy val s3Client: S3Client = {
    val awsCredentials =
      AwsBasicCredentials.create(storageAccountInfo.applicationId, storageAccountInfo.applicationSecret)
    val s3 = S3Client
      .builder()
      .region(Region.EU_CENTRAL_1)
      .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
      .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
      .build()
    s3
  }

  override def store(id: UUID, fileParts: (FileInfo, File)): Future[OnboardingFilePath] = Future.fromTry {

    Try {
      val s3Key = createS3Key(
        id.toString,
        contentType = fileParts._1.getContentType.toString(),
        fileName = fileParts._1.getFileName
      )
      val objectRequest =
        PutObjectRequest.builder
          .bucket(storageAccountInfo.container)
          .key(s3Key)
          .build

      val _ = s3Client.putObject(objectRequest, RequestBody.fromFile(Paths.get(fileParts._2.getPath)))

      s3Key
    }
  }

  private def createS3Key(tokenId: String, contentType: String, fileName: String): String =
    s"parties/docs/$tokenId/${contentType}/$fileName"

  override def get(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      val getObjectRequest: GetObjectRequest =
        GetObjectRequest.builder.bucket(storageAccountInfo.container).key(filePath).build
      val s3Object: ResponseBytes[GetObjectResponse] = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes)
      val inputStream: InputStream                   = s3Object.asInputStream()
      val outputStream: ByteArrayOutputStream        = new ByteArrayOutputStream()
      val _                                          = inputStream.transferTo(outputStream)
      outputStream
    }
  }

  override def delete(path: String): Future[Boolean] = {
    Try {
      s3Client.deleteObject(
        DeleteObjectRequest.builder
          .bucket(storageAccountInfo.container)
          .key(path)
          .build()
      )
    }.fold(error => Future.failed[Boolean](error), _ => Future.successful(true))
  }

}
