package it.pagopa.interop.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.interop.partymanagement.model._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object InstitutionsExternalApiServiceData {

  lazy final val institutionUuid1 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9290")

  lazy final val externalId1 = "__ext_id1"
  lazy final val originId1   = "origin_id1"

  lazy final val institution1 = Institution(
    externalId = externalId1,
    originId = originId1,
    description = "Institutions One",
    digitalAddress = "mail1@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    id = institutionUuid1,
    attributes = Seq.empty,
    origin = "IPA",
    institutionType = Option("PA"),
    products = Map.empty,
    geographicTaxonomies = Seq.empty
  )

  def prepareTest()(implicit
    as: ActorSystem,
    mp: Marshaller[InstitutionSeed, MessageEntity],
    ec: ExecutionContext
  ): HttpResponse = {
    (() => uuidSupplier.get).expects().returning(institutionUuid1).once()

    (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

    val institutionSeed = InstitutionSeed(
      externalId = institution1.externalId,
      originId = institution1.originId,
      description = institution1.description,
      digitalAddress = institution1.digitalAddress,
      address = institution1.address,
      zipCode = institution1.zipCode,
      taxCode = institution1.taxCode,
      attributes = institution1.attributes,
      origin = institution1.origin,
      institutionType = institution1.institutionType,
      products = None
    )
    val orgRequestData  = Await.result(Marshal(institutionSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createInstitution(orgRequestData)
  }

}
