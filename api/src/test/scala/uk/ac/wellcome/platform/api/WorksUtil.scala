package uk.ac.wellcome.platform.api

import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models._

trait WorksUtil {

  val canonicalId = "1234"
  val title = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"

  val period = Period("the past")
  val agent = Agent("a person")

  def createWorks(count: Int): Seq[Work] =
    (1 to count).map(
      (idx: Int) =>
        workWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}"),
          List(defaultItem)
      ))

  def workWith(canonicalId: String, title: String): Work =
    Work(
      canonicalId = Some(canonicalId),
      identifiers = List(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")
      ),
      title = title
    )

  def workWith(
    canonicalId: String,
    title: String,
    identifiers: List[SourceIdentifier] = List(),
    items: List[Item] = List()
  ): Work =
    Work(
      canonicalId = Some(canonicalId),
      identifiers = identifiers,
      title = title,
      items = items
    )

  def identifiedWorkWith(
    canonicalId: String,
    title: String,
    thumbnail: Location
  ): Work =
    Work(
      canonicalId = Some(canonicalId),
      identifiers = List(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")
      ),
      title = title,
      thumbnail = Some(thumbnail)
    )

  def workWith(canonicalId: String,
               title: String,
               description: String,
               lettering: String,
               createdDate: Period,
               creator: Agent,
               items: List[Item]): Work = Work(
    canonicalId = Some(canonicalId),
    identifiers =
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
    title = title,
    description = Some(description),
    lettering = Some(lettering),
    createdDate = Some(createdDate),
    creators = List(creator),
    items = items
  )

  def defaultItem: Item = {
    itemWith(
      "item-canonical-id",
      defaultSourceIdentifier,
      defaultLocation
    )
  }

  def defaultSourceIdentifier = {
    SourceIdentifier("miro-image-number", "M0000001")
  }

  def defaultLocation: Location = {
    locationWith(
      Some("https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json"),
      License_CCBY)
  }

  def itemWith(canonicalId: String,
               identifier: SourceIdentifier,
               location: Location): Item = {
    Item(
      canonicalId = Some(canonicalId),
      List(
        identifier
      ),
      List(
        location
      )
    )
  }

  def locationWith(url: Option[String], license: BaseLicense): Location = {
    Location(
      "iiif-image",
      url,
      license
    )
  }
}
