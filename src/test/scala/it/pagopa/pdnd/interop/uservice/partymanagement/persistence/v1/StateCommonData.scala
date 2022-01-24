package it.pagopa.pdnd.interop.uservice.partymanagement.persistence.v1

import java.time.OffsetDateTime
import java.util.UUID

object StateCommonData {
  val start     = OffsetDateTime.now()
  val end       = OffsetDateTime.now()
  val createdAt = OffsetDateTime.now()
  val updatedAt = OffsetDateTime.now()

  val personPartyId      = UUID.randomUUID()
  val institutionPartyId = UUID.randomUUID()

  val externalId2    = UUID.randomUUID()
  val description    = "description"
  val digitalAddress = "digitalAddress"
  val taxCode        = "taxCode"
  val attributes     = Seq("a", "b")

  val productId        = "productId"
  val productRole      = "productRole"
  val productCreatedAt = OffsetDateTime.now()

  val noneTestId         = UUID.randomUUID()
  val managerActiveId    = UUID.randomUUID()
  val managerPendingId   = UUID.randomUUID()
  val managerRejectedId  = UUID.randomUUID()
  val managerSuspendedId = UUID.randomUUID()
  val managerDeletedId   = UUID.randomUUID()

  val delegateActiveId    = UUID.randomUUID()
  val delegatePendingId   = UUID.randomUUID()
  val delegateRejectedId  = UUID.randomUUID()
  val delegateSuspendedId = UUID.randomUUID()
  val delegateDeletedId   = UUID.randomUUID()

  val subDelegateActiveId    = UUID.randomUUID()
  val subDelegatePendingId   = UUID.randomUUID()
  val subDelegateRejectedId  = UUID.randomUUID()
  val subDelegateSuspendedId = UUID.randomUUID()
  val subDelegateDeletedId   = UUID.randomUUID()

  val operatorActiveId    = UUID.randomUUID()
  val operatorPendingId   = UUID.randomUUID()
  val operatorRejectedId  = UUID.randomUUID()
  val operatorSuspendedId = UUID.randomUUID()
  val operatorDeletedId   = UUID.randomUUID()

  val filePath          = "filePath"
  val fileName          = "fileName"
  val contentType       = "contentType"
  val onboardingTokenId = UUID.randomUUID()

  val tokenId  = UUID.randomUUID()
  val validity = OffsetDateTime.now()
  val checksum = "checksum"
  val version  = "version"
  val path     = "path"
}
