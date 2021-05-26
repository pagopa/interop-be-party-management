package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.events.{
  TokenAddedV2,
  TokenConsumedV2,
  TokenInvalidatedV2
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{TokenAdded, TokenConsumed, TokenInvalidated}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.utils._

package object v2 {
  implicit def tokenAddedV2PersistEventDeserializer: PersistEventDeserializer[TokenAddedV2, TokenAdded] = event =>
    getToken(event.token).map(TokenAdded)

  implicit def tokenAddedV2PersistEventSerializer: PersistEventSerializer[TokenAdded, TokenAddedV2] =
    event => getTokenV2(event.token).map(TokenAddedV2.of)

  implicit def tokenConsumedV2PersistEventDeserializer: PersistEventDeserializer[TokenConsumedV2, TokenConsumed] =
    event => getToken(event.token).map(TokenConsumed)

  implicit def tokenConsumedV2PersistEventSerializer: PersistEventSerializer[TokenConsumed, TokenConsumedV2] =
    event => getTokenV2(event.token).map(TokenConsumedV2.of)

  implicit def tokenInvalidatedV2PersistEventDeserializer
    : PersistEventDeserializer[TokenInvalidatedV2, TokenInvalidated] =
    event => getToken(event.token).map(TokenInvalidated)

  implicit def tokenInvalidatedV2PersistEventSerializer: PersistEventSerializer[TokenInvalidated, TokenInvalidatedV2] =
    event => getTokenV2(event.token).map(TokenInvalidatedV2.of)
}
