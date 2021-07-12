package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Delegate, Manager, PartyRelationShipId, Token}

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object TokenApiServiceData {

  // format: off

  lazy final val createTokenUuid0 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val createTokenUuid1 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val createTokenUuid2 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9212"
  lazy final val createTokenUuid3 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9213"
  lazy final val createTokenUuid4 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9214"
  lazy final val createTokenUuid5 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9215"

  lazy final val tokenSeedId1 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val tokenSeedId2 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val tokenSeedId3 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9212"

  lazy final val taxCode1 = "RSSMRA75L01H501H"
  lazy final val taxCode2 = "RSSMRA75L01H501I"
  lazy final val taxCode3 = "RSSMRA75L01H501J"

  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Mascetti", name = "Raffaello")
  lazy final val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Melandri", name = "Ranbaudo")
  lazy final val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Perozzi", name = "Giorgio")

  lazy final val institutionId1 = "id9"
  lazy final val institutionId2 = "id10"
  lazy final val institutionId3 = "id13"

  lazy final val organizationSeed1 = OrganizationSeed(institutionId1, "Institutions Nine", "Raffaello Mascetti", "mail9@mail.org", Seq.empty)
  lazy final val organizationSeed2 = OrganizationSeed(institutionId2, "Institutions Ten", "Melandri Ranbaudo", "mail10@mail.org", Seq.empty)
  lazy final val organizationSeed3 = OrganizationSeed(institutionId3, "Institutions Eleven", "Perozzi Giorgio", "mail11@mail.org", Seq.empty)

  lazy final val relationShip1 = RelationShip(from = taxCode1, to = institutionId1, role = "Manager", None)
  lazy final val relationShip2 = RelationShip(from = taxCode1, to = institutionId1, role = "Delegate", None)
  lazy final val relationShip3 = RelationShip(from = taxCode2, to = institutionId2, role = "Manager", None)
  lazy final val relationShip4 = RelationShip(from = taxCode2, to = institutionId2, role = "Delegate", None)
  lazy final val relationShip5 = RelationShip(from = taxCode3, to = institutionId3, role = "Manager", None)
  lazy final val relationShip6 = RelationShip(from = taxCode3, to = institutionId3, role = "Delegate", None)

  lazy final val partyRelationShipId1 = PartyRelationShipId(from = UUID.fromString(createTokenUuid2), to = UUID.fromString(createTokenUuid3), role = Manager)
  lazy final val partyRelationShipId2 = PartyRelationShipId(from = UUID.fromString(createTokenUuid2), to = UUID.fromString(createTokenUuid3), role = Delegate)
  lazy final val partyRelationShipId3 = PartyRelationShipId(from = UUID.fromString(createTokenUuid4), to = UUID.fromString(createTokenUuid5), role = Manager)
  lazy final val partyRelationShipId4 = PartyRelationShipId(from = UUID.fromString(createTokenUuid4), to = UUID.fromString(createTokenUuid5), role = Delegate)

  lazy val tokenSeed1: TokenSeed = TokenSeed(seed = tokenSeedId2, relationShips = RelationShips(Seq(relationShip3, relationShip4)), "checksum")
  lazy val tokenSeed2: TokenSeed = TokenSeed(seed = tokenSeedId3, relationShips = RelationShips(Seq(relationShip5, relationShip6)), "checksum")

  lazy val token1: Token = Token.generate(tokenSeed1, Seq(partyRelationShipId1, partyRelationShipId2)).toOption.get
  lazy val token2: Token = Token.generate(tokenSeed2, Seq(partyRelationShipId3, partyRelationShipId4)).toOption.get

  def prepareTest(
    personSeed: PersonSeed,
    organizationSeed: OrganizationSeed,
    relationShipOne: RelationShip,
    relationShipTwo: RelationShip
  )(implicit
    as: ActorSystem,
    mp: Marshaller[PersonSeed, MessageEntity],
    mo: Marshaller[OrganizationSeed, MessageEntity],
    mr: Marshaller[RelationShip, MessageEntity],
    ec: ExecutionContext
  ): HttpResponse = {
    val personRequestData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createPerson(personRequestData)

    val orgRequestData = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createOrganization(orgRequestData)

    val rlRequestData1 = Await.result(Marshal(relationShipOne).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createRelationShip(rlRequestData1)

    val rlRequestData2 = Await.result(Marshal(relationShipTwo).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createRelationShip(rlRequestData2)

    Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$url/relationships/${relationShipOne.from}",
          method = HttpMethods.GET,
          headers = authorization
        )
      ),
      Duration.Inf
    )

  }

}
