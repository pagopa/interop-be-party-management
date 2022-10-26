package it.pagopa.interop.commons.queue.kafka

import spray.json.JsonWriter

import scala.concurrent.Future

trait KafkaPublisher {
  def send[T](message: T)(implicit messageSerializer: JsonWriter[T]): Future[Unit]
}
