package it.pagopa.interop.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.partymanagement.api.NewDesignExposureApiMarshaller
import it.pagopa.interop.partymanagement.model.{NewDesignUser, Problem}
import spray.json._

object NewDesignExposureApiMarshallerImpl
    extends NewDesignExposureApiMarshaller
    with SprayJsonSupport
    with DefaultJsonProtocol {

  override implicit def toEntityMarshallerNewDesignUserarray: ToEntityMarshaller[Seq[NewDesignUser]] =
    sprayJsonMarshaller[Seq[NewDesignUser]]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]

}
