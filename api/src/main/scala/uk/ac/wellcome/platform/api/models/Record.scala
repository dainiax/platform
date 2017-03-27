package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.searches.RichSearchHit

case class Record(
  @JsonProperty("type") ontologyType: String = "Work",
  id: String,
  title: String
//  materialType: String,
//  date: Option[String],
//  acquisition: Option[String]
)
case object Record {
  def apply(hit: RichSearchHit): Record = {
    val data: Map[String, AnyRef] =
      hit.sourceAsMap.filter(o => o._2 != null)

    Record(
      id = data("AltRefNo").toString,
      title = data("Title").toString
//      materialType=data("Material").toString,
//      date=data.get("Date").map(_.toString),
//      acquisition=data.get("Acquisition").map(_.toString)
    )
  }
}