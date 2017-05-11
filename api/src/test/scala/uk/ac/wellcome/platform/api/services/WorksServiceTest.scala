package uk.ac.wellcome.platform.api.services

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
import uk.ac.wellcome.platform.api.WorksUtil
import uk.ac.wellcome.platform.api.models.DisplayWork
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

import scala.concurrent.ExecutionContext.Implicits.global

class WorksServiceTest
    extends FunSpec
    with IndexedElasticSearchLocal
    with Matchers
    with ScalaFutures
    with WorksUtil {

  val searchService =
    new ElasticSearchService(indexName, itemType, elasticClient)

  val worksService =
    new WorksService(10, searchService)

  it("should return the records in Elasticsearch") {
    val works = createIdentifiedWorks(2)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture = worksService.listWorks()

    displayWorksFuture map { displayWork =>
      displayWork.results should have size 2
      displayWork.results.head shouldBe DisplayWork("Work",
                                                    works(0).canonicalId,
                                                    works(0).work.label)
      displayWork.results.tail.head shouldBe DisplayWork("Work",
                                                         works(1).canonicalId,
                                                         works(1).work.label)
    }
  }

  it("should get a DisplayWork by id") {
    val works = createIdentifiedWorks(1)

    insertIntoElasticSearch(works: _*)

    val recordsFuture = worksService.findWorkById(works.head.canonicalId)

    whenReady(recordsFuture) { records =>
      records.isDefined shouldBe true
      records.get shouldBe convertWorkToDisplayWork(works.head)
    }
  }

  it("should only find results that match a query if doing a full-text search") {
    val workDodo = identifiedWorkWith(
      canonicalId = "1234",
      label = "A drawing of a dodo"
    )
    val workMouse = identifiedWorkWith(
      canonicalId = "5678",
      label = "A mezzotint of a mouse"
    )

    insertIntoElasticSearch(workDodo, workMouse)

    val searchForCat = worksService.searchWorks("cat")

    whenReady(searchForCat) { works =>
      works.results should have size 0
    }

    val searchForDodo = worksService.searchWorks("dodo")
    whenReady(searchForDodo) { works =>
      works.results should have size 1
      works.results.head shouldBe DisplayWork("Work",
                                              workDodo.canonicalId,
                                              workDodo.work.label)
    }
  }

  it("should return a future of None if it cannot get arecord by id") {
    val recordsFuture = worksService.findWorkById("1234")

    whenReady(recordsFuture) { record =>
      record shouldBe None
    }
  }

  it("should return 0 pages when no results are available") {
    val displayWorksFuture = worksService.listWorks(pageSize = 10)

    whenReady(displayWorksFuture) { works =>
      works.totalPages shouldBe 0
    }
  }

  it(
    "should return the correct totalPages for the number of results and pageSize") {
    val works = createIdentifiedWorks(2)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture = worksService.listWorks(pageSize = 1)

    whenReady(displayWorksFuture) { works =>
      works.totalPages shouldBe 2
    }
  }

  it("should return the correct number of results per page for the pageSize") {
    val works = createIdentifiedWorks(3)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture = worksService.listWorks(pageSize = 2)

    whenReady(displayWorksFuture) { works =>
      works.results.length shouldBe 2
    }
  }

  it("should display the correct page when asked") {
    val works = createIdentifiedWorks(3)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture = worksService.listWorks(pageSize = 1, pageNumber = 2)

    whenReady(displayWorksFuture) { receivedWorks =>
      receivedWorks.results.head shouldBe convertWorkToDisplayWork(works(1))
    }
  }

  it("should return an empty result set when asked for a page that does not exist") {
    val works = createIdentifiedWorks(3)

    insertIntoElasticSearch(works: _*)

    val displayWorksFuture = worksService.listWorks(pageSize = 1, pageNumber = 4)

    whenReady(displayWorksFuture) { receivedWorks =>
      receivedWorks.results shouldBe empty
    }
  }

  it(
    "should not throw an exception if passed an invalid query string for full-text search") {
    val workEmu = identifiedWorkWith(
      canonicalId = "1234",
      label = "An etching of an emu"
    )
    insertIntoElasticSearch(workEmu)

    val searchForEmu = worksService.searchWorks(
      "emu \"unmatched quotes are a lexical error in the Elasticsearch parser"
    )

    whenReady(searchForEmu) { works =>
      works.results should have size 1
      works.results.head shouldBe DisplayWork("Work",
                                              workEmu.canonicalId,
                                              workEmu.work.label)
    }
  }
}