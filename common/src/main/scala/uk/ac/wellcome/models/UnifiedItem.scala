package uk.ac.wellcome.models

import com.sksamuel.elastic4s._
import uk.ac.wellcome.utils.JsonUtil

case class Identifier(CanonicalID: String, MiroID: String)

case class IdentifiedUnifiedItem(canonicalId: String, unifiedItem: UnifiedItem)

case class SourceIdentifier(source: String, sourceId: String, value: String)

case class UnifiedItem(
  id: String,
  identifiers: List[SourceIdentifier],
  accessStatus: Option[String]
)
object UnifiedItem extends Indexable[UnifiedItem] {
  override def json(t: UnifiedItem): String =
    JsonUtil.toJson(t).get
}
