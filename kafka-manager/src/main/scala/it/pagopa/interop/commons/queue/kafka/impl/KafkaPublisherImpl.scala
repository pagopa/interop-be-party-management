package it.pagopa.interop.commons.queue.kafka.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class KafkaPublisherImpl(
  system: ActorSystem[_],
  topic: String,
  bootstrapServers: String,
  properties: Map[String, String]
)(implicit ec: ExecutionContext)
    extends KafkaPublisher {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
      .withProperties(properties)

  private val sendProducer = SendProducer(producerSettings)(system.toClassic)

  def send[T](message: T)(implicit messageSerializer: JsonWriter[T]): Future[String] = {
    val messageString  = message.toJson.toString
    val producerRecord = new ProducerRecord[String, String](topic, null, messageString)
    val result         = sendProducer.send(producerRecord).map { recordMetadata =>
      logger.debug("Published message [{}] to topic/partition {}/{}", messageString, topic, recordMetadata.partition)
      logger.info("Published message to topic/partition {}/{}", topic, recordMetadata.partition)
      "OK"
    }
    result
  }
}
