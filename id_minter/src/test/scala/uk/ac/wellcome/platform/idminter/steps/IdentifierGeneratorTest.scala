package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import scalikejdbc._
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.IdentifiersMysqlLocal

import scala.util.{Failure, Success}

class IdentifierGeneratorTest
    extends FunSpec
    with IdentifiersMysqlLocal
    with Matchers
    with MockitoSugar {

  private val metricsSender =
    new MetricsSender("id_minter_test_metrics", mock[AmazonCloudWatch])
  private val knownIdentifierSchemes =
    List(IdentifierSchemes.miroImageNumber, IdentifierSchemes.calmAltRefNo)
  val identifierGenerator = new IdentifierGenerator(
    new IdentifiersDao(DB.connect(), identifiersTable),
    metricsSender,
    knownIdentifierSchemes.mkString(","))

  it(
    "should search the miro id in the database and return the canonical id if it finds it") {
    withSQL {
      insert
        .into(identifiersTable)
        .namedValues(identifiersTable.column.CanonicalID -> "5678",
                     identifiersTable.column.MiroID -> "1234",
                     identifiersTable.column.ontologyType -> "Work")
    }.update().apply()

    val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234")),
      "Work")

    triedId shouldBe Success("5678")
  }

  it(
    "should generate an id and save it in the database if a record doesn't already exist") {
    val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "1234")),
      "Work")

    triedId shouldBe a[Success[String]]
    val id = triedId.get
    id should not be (empty)
    val i = identifiersTable.i
    val maybeIdentifier = withSQL {
      select.from(identifiersTable as i).where.eq(i.MiroID, "1234")
    }.map(Identifier(i)).single.apply()
    maybeIdentifier shouldBe defined
    maybeIdentifier.get shouldBe Identifier(CanonicalID = id, MiroID = "1234")
  }

  it(
    "should fail if the identifier does not contain a known identifierScheme in the list of Identifiers") {
    val triedGeneratingId = identifierGenerator.retrieveOrGenerateCanonicalId(
      List(SourceIdentifier("not-a-known-identifier-scheme", "1234")),
      "Work")

    triedGeneratingId shouldBe a[Failure[Exception]]
    val exception = triedGeneratingId.failed.get.asInstanceOf[Exception]
    exception.getMessage shouldBe s"identifiers list did not contain a known identifierScheme"
  }

  it(
    "should return a failure if it fails inserting the identifier in the database") {
    val sourceIdentifiers = List(
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        value = "1234"
      )
    )
    val identifiersDao = mock[IdentifiersDao]
    val identifierGenerator =
      new IdentifierGenerator(identifiersDao,
                              metricsSender,
                              knownIdentifierSchemes.mkString(","))

    val triedLookup = identifiersDao.lookupID(
      sourceIdentifiers = sourceIdentifiers,
      ontologyType = "Work"
    )
    when(triedLookup)
      .thenReturn(Success(None))
    val expectedException = new Exception("Noooo")
    when(identifiersDao.saveIdentifier(any[Identifier]()))
      .thenReturn(Failure(expectedException))

    val triedGeneratingId =
      identifierGenerator.retrieveOrGenerateCanonicalId(sourceIdentifiers,
                                                        "Work")
    triedGeneratingId shouldBe a[Failure[Exception]]
    triedGeneratingId.failed.get shouldBe expectedException
  }

  it("""should filter identifiers using the known
      IdentifierSchemes before sending them to the dao""") {

    val miroId = "1234"
    val calmAltRefNo = "5678"
    val knownSchemeIdentifiers = List(
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        value = miroId
      ),
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.calmAltRefNo,
        value = calmAltRefNo
      )
    )
    val sourceIdentifiers = knownSchemeIdentifiers :+ SourceIdentifier(
      identifierScheme = "not-a-known-identifier-scheme",
      value = "jshfgh"
    )

    val triedId =
      identifierGenerator.retrieveOrGenerateCanonicalId(sourceIdentifiers,
                                                        "Work")

    triedId shouldBe a[Success[String]]
    val id = triedId.get
    id should not be (empty)
    val i = identifiersTable.i
    val maybeIdentifier = withSQL {
      select.from(identifiersTable as i).where.eq(i.MiroID, miroId)
    }.map(Identifier(i)).single.apply()
    maybeIdentifier shouldBe defined
    maybeIdentifier.get shouldBe Identifier(CanonicalID = id,
                                            MiroID = miroId,
                                            CalmAltRefNo = calmAltRefNo)
  }

  it("should preserve the ontologyType when generating a new identifier") {
    val ontologyType = "Item"
    val miroId = "1234"
    val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
      List(SourceIdentifier(IdentifierSchemes.miroImageNumber, miroId)),
      ontologyType)

    triedId shouldBe a[Success[String]]
    val id = triedId.get
    id should not be (empty)
    val i = identifiersTable.i
    val maybeIdentifier = withSQL {
      select.from(identifiersTable as i).where.eq(i.MiroID, miroId)
    }.map(Identifier(i)).single.apply()
    maybeIdentifier shouldBe defined
    maybeIdentifier.get shouldBe Identifier(CanonicalID = id,
                                            MiroID = miroId,
                                            ontologyType = ontologyType)
  }
}
