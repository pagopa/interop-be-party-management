package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.partymanagement.server.StringDirectives
import it.pagopa.pdnd.interop.uservice.partymanagement.server.MultipartDirectives
import it.pagopa.pdnd.interop.uservice.partymanagement.server.FileField
import it.pagopa.pdnd.interop.uservice.partymanagement.server.PartsAndFiles
import it.pagopa.pdnd.interop.uservice.partymanagement.model.BulkOrganizations
import it.pagopa.pdnd.interop.uservice.partymanagement.model.BulkPartiesSeed
import java.io.File
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Organization
import it.pagopa.pdnd.interop.uservice.partymanagement.model.OrganizationSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Person
import it.pagopa.pdnd.interop.uservice.partymanagement.model.PersonSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Problem
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Products
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Relationship
import it.pagopa.pdnd.interop.uservice.partymanagement.model.RelationshipSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Relationships
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenText
import java.util.UUID
import scala.util.Try
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.server.directives.FileInfo

class PartyApi(
  partyService: PartyApiService,
  partyMarshaller: PartyApiMarshaller,
  wrappingDirective: Directive1[Seq[(String, String)]]
) extends MultipartDirectives
    with StringDirectives {

  import partyMarshaller._

  lazy val route: Route =
    path("relationships" / Segment / "activate") { (relationshipId) =>
      post {
        wrappingDirective { implicit contexts =>
          partyService.activatePartyRelationshipById(relationshipId = relationshipId)
        }
      }
    } ~
      path("organizations" / Segment / "attributes") { (id) =>
        post {
          wrappingDirective { implicit contexts =>
            entity(as[Seq[String]]) { requestBody =>
              partyService.addOrganizationAttributes(id = id, requestBody = requestBody)
            }
          }
        }
      } ~
      path("organizations" / Segment / "products") { (id) =>
        post {
          wrappingDirective { implicit contexts =>
            entity(as[Products]) { products =>
              partyService.addOrganizationProducts(id = id, products = products)
            }
          }
        }
      } ~
      path("relationships" / Segment / "products") { (relationshipId) =>
        post {
          wrappingDirective { implicit contexts =>
            entity(as[Products]) { products =>
              partyService.addRelationshipProducts(relationshipId = relationshipId, products = products)
            }
          }
        }
      } ~
      path("bulk" / "organizations") {
        post {
          wrappingDirective { implicit contexts =>
            entity(as[BulkPartiesSeed]) { bulkPartiesSeed =>
              partyService.bulkOrganizations(bulkPartiesSeed = bulkPartiesSeed)
            }
          }
        }
      } ~
      path("tokens" / Segment) { (token) =>
        post {
          wrappingDirective { implicit contexts =>
            formAndFiles(FileField("doc")) { partsAndFiles =>
              val routes: Try[Route] = for {
                doc <- optToTry(partsAndFiles.files.get("doc"), s"File doc missing")
              } yield {
                implicit val vp: StringValueProvider = partsAndFiles.form ++ contexts.toMap
                partyService.consumeToken(token = token, doc = doc)
              }
              routes.fold[Route](t => reject(MalformedRequestContentRejection("Missing file.", t)), identity)
            }
          }
        }
      } ~
      path("organizations") {
        post {
          wrappingDirective { implicit contexts =>
            entity(as[OrganizationSeed]) { organizationSeed =>
              partyService.createOrganization(organizationSeed = organizationSeed)
            }
          }
        }
      } ~
      path("persons") {
        post {
          wrappingDirective { implicit contexts =>
            entity(as[PersonSeed]) { personSeed =>
              partyService.createPerson(personSeed = personSeed)
            }
          }
        }
      } ~
      path("relationships") {
        post {
          wrappingDirective { implicit contexts =>
            entity(as[RelationshipSeed]) { relationshipSeed =>
              partyService.createRelationship(relationshipSeed = relationshipSeed)
            }
          }
        }
      } ~
      path("tokens") {
        post {
          wrappingDirective { implicit contexts =>
            entity(as[TokenSeed]) { tokenSeed =>
              partyService.createToken(tokenSeed = tokenSeed)
            }
          }
        }
      } ~
      path("relationships" / Segment) { (relationshipId) =>
        delete {
          wrappingDirective { implicit contexts =>
            partyService.deleteRelationshipById(relationshipId = relationshipId)
          }
        }
      } ~
      path("organizations" / Segment) { (id) =>
        head {
          wrappingDirective { implicit contexts =>
            partyService.existsOrganizationById(id = id)
          }
        }
      } ~
      path("persons" / Segment) { (id) =>
        head {
          wrappingDirective { implicit contexts =>
            partyService.existsPersonById(id = id)
          }
        }
      } ~
      path("organizations" / "external" / Segment) { (id) =>
        get {
          wrappingDirective { implicit contexts =>
            partyService.getOrganizationByExternalId(id = id)
          }
        }
      } ~
      path("organizations" / Segment) { (id) =>
        get {
          wrappingDirective { implicit contexts =>
            partyService.getOrganizationById(id = id)
          }
        }
      } ~
      path("organizations" / Segment / "attributes") { (id) =>
        get {
          wrappingDirective { implicit contexts =>
            partyService.getPartyAttributes(id = id)
          }
        }
      } ~
      path("persons" / Segment) { (id) =>
        get {
          wrappingDirective { implicit contexts =>
            partyService.getPersonById(id = id)
          }
        }
      } ~
      path("relationships" / Segment) { (relationshipId) =>
        get {
          wrappingDirective { implicit contexts =>
            partyService.getRelationshipById(relationshipId = relationshipId)
          }
        }
      } ~
      path("relationships") {
        get {
          wrappingDirective { implicit contexts =>
            parameters("from".as[String].?, "to".as[String].?, "productRole".as[String].?) { (from, to, productRole) =>
              partyService.getRelationships(from = from, to = to, productRole = productRole)
            }
          }
        }
      } ~
      path("tokens" / Segment) { (token) =>
        delete {
          wrappingDirective { implicit contexts =>
            partyService.invalidateToken(token = token)
          }
        }
      } ~
      path("relationships" / Segment / "suspend") { (relationshipId) =>
        post {
          wrappingDirective { implicit contexts =>
            partyService.suspendPartyRelationshipById(relationshipId = relationshipId)
          }
        }
      } ~
      path("tokens" / Segment) { (token) =>
        head {
          wrappingDirective { implicit contexts =>
            partyService.verifyToken(token = token)
          }
        }
      }
}

trait PartyApiService {
  def activatePartyRelationshipById204: Route =
    complete((204, "Relationship activated"))
  def activatePartyRelationshipById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: Relationship activated
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  def activatePartyRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

  def addOrganizationAttributes200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route =
    complete((200, responseOrganization))
  def addOrganizationAttributes404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Organization
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  def addOrganizationAttributes(id: String, requestBody: Seq[String])(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def addOrganizationProducts200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route =
    complete((200, responseOrganization))
  def addOrganizationProducts404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Organization
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  def addOrganizationProducts(id: String, products: Products)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def addRelationshipProducts200(responseRelationship: Relationship)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship]
  ): Route =
    complete((200, responseRelationship))
  def addRelationshipProducts404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Relationship
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  def addRelationshipProducts(relationshipId: String, products: Products)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def bulkOrganizations200(responseBulkOrganizations: BulkOrganizations)(implicit
    toEntityMarshallerBulkOrganizations: ToEntityMarshaller[BulkOrganizations]
  ): Route =
    complete((200, responseBulkOrganizations))
  def bulkOrganizations400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def bulkOrganizations404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: collection of organizations, DataType: BulkOrganizations
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Organizations not found, DataType: Problem
    */
  def bulkOrganizations(bulkPartiesSeed: BulkPartiesSeed)(implicit
    toEntityMarshallerBulkOrganizations: ToEntityMarshaller[BulkOrganizations],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def consumeToken201: Route =
    complete((201, "successful operation"))
  def consumeToken400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def consumeToken(token: String, doc: (FileInfo, File))(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def createOrganization201(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route =
    complete((201, responseOrganization))
  def createOrganization400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 201, Message: successful operation, DataType: Organization
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def createOrganization(organizationSeed: OrganizationSeed)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def createPerson201(responsePerson: Person)(implicit toEntityMarshallerPerson: ToEntityMarshaller[Person]): Route =
    complete((201, responsePerson))
  def createPerson400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 201, Message: successful operation, DataType: Person
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def createPerson(personSeed: PersonSeed)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def createRelationship201(responseRelationship: Relationship)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship]
  ): Route =
    complete((201, responseRelationship))
  def createRelationship400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 201, Message: Created Relationship, DataType: Relationship
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def createRelationship(relationshipSeed: RelationshipSeed)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def createToken201(responseTokenText: TokenText)(implicit
    toEntityMarshallerTokenText: ToEntityMarshaller[TokenText]
  ): Route =
    complete((201, responseTokenText))
  def createToken400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))

  /** Code: 201, Message: successful operation, DataType: TokenText
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def createToken(tokenSeed: TokenSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTokenText: ToEntityMarshaller[TokenText],
    contexts: Seq[(String, String)]
  ): Route

  def deleteRelationshipById204: Route =
    complete((204, "relationship deleted"))
  def deleteRelationshipById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def deleteRelationshipById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: relationship deleted
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  def deleteRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

  def existsOrganizationById200: Route =
    complete((200, "successful operation"))
  def existsOrganizationById404: Route =
    complete((404, "Organization not found"))

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Organization not found
    */
  def existsOrganizationById(id: String): Route

  def existsPersonById200: Route =
    complete((200, "Person exists"))
  def existsPersonById404: Route =
    complete((404, "Person not found"))

  /** Code: 200, Message: Person exists
    * Code: 404, Message: Person not found
    */
  def existsPersonById(id: String): Route

  def getOrganizationByExternalId200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route =
    complete((200, responseOrganization))
  def getOrganizationByExternalId400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def getOrganizationByExternalId404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Organization, DataType: Organization
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  def getOrganizationByExternalId(id: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def getOrganizationById200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route =
    complete((200, responseOrganization))
  def getOrganizationById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def getOrganizationById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Organization, DataType: Organization
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  def getOrganizationById(id: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def getPartyAttributes200(responseStringarray: Seq[String])(implicit
    toEntityMarshallerStringarray: ToEntityMarshaller[Seq[String]]
  ): Route =
    complete((200, responseStringarray))
  def getPartyAttributes400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def getPartyAttributes404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Party Attributes, DataType: Seq[String]
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Party not found, DataType: Problem
    */
  def getPartyAttributes(
    id: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

  def getPersonById200(responsePerson: Person)(implicit toEntityMarshallerPerson: ToEntityMarshaller[Person]): Route =
    complete((200, responsePerson))
  def getPersonById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def getPersonById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Person, DataType: Person
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Person not found, DataType: Problem
    */
  def getPersonById(id: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def getRelationshipById200(responseRelationship: Relationship)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship]
  ): Route =
    complete((200, responseRelationship))
  def getRelationshipById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def getRelationshipById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Relationship
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  def getRelationshipById(relationshipId: String)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def getRelationships200(responseRelationships: Relationships)(implicit
    toEntityMarshallerRelationships: ToEntityMarshaller[Relationships]
  ): Route =
    complete((200, responseRelationships))
  def getRelationships400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Relationships
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def getRelationships(from: Option[String], to: Option[String], productRole: Option[String])(implicit
    toEntityMarshallerRelationships: ToEntityMarshaller[Relationships],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route

  def invalidateToken200: Route =
    complete((200, "successful operation"))
  def invalidateToken400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 200, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def invalidateToken(
    token: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

  def suspendPartyRelationshipById204: Route =
    complete((204, "Relationship suspended"))
  def suspendPartyRelationshipById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: Relationship suspended
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  def suspendPartyRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

  def verifyToken200: Route =
    complete((200, "successful operation"))
  def verifyToken404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((404, responseProblem))
  def verifyToken400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Token not found, DataType: Problem
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  def verifyToken(
    token: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route

}

trait PartyApiMarshaller {
  implicit def fromEntityUnmarshallerPersonSeed: FromEntityUnmarshaller[PersonSeed]

  implicit def fromEntityUnmarshallerStringList: FromEntityUnmarshaller[Seq[String]]

  implicit def fromEntityUnmarshallerOrganizationSeed: FromEntityUnmarshaller[OrganizationSeed]

  implicit def fromEntityUnmarshallerRelationshipSeed: FromEntityUnmarshaller[RelationshipSeed]

  implicit def fromEntityUnmarshallerProducts: FromEntityUnmarshaller[Products]

  implicit def fromEntityUnmarshallerBulkPartiesSeed: FromEntityUnmarshaller[BulkPartiesSeed]

  implicit def fromEntityUnmarshallerTokenSeed: FromEntityUnmarshaller[TokenSeed]

  implicit def toEntityMarshallerPerson: ToEntityMarshaller[Person]

  implicit def toEntityMarshallerRelationships: ToEntityMarshaller[Relationships]

  implicit def toEntityMarshallerBulkOrganizations: ToEntityMarshaller[BulkOrganizations]

  implicit def toEntityMarshallerRelationship: ToEntityMarshaller[Relationship]

  implicit def toEntityMarshallerOrganization: ToEntityMarshaller[Organization]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

  implicit def toEntityMarshallerTokenText: ToEntityMarshaller[TokenText]

}
