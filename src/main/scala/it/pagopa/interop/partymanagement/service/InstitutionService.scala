package it.pagopa.interop.partymanagement.service

import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.party.Party

import java.util.UUID
import scala.concurrent.Future

trait InstitutionService {
  def getInstitutionById(institutionId: UUID)(implicit timeout: Timeout): Future[Option[Party]]
}
