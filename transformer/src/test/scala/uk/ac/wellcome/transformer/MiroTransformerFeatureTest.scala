package uk.ac.wellcome.transformer

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.miro.MiroTransformable
import uk.ac.wellcome.test.utils.{MessageInfo, MiroTransformableWrapper}
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest
    extends FunSpec
    with TransformerFeatureTest
    with Matchers
    with MiroTransformableWrapper {

  val queueUrl: String = createQueueAndReturnUrl("test_miro_transformer")
  override val flags: Map[String, String] = Map(
    "transformer.source" -> "MiroData",
    "aws.region" -> "eu-west-1",
    "aws.sqs.queue.url" -> queueUrl,
    "aws.sqs.waitTime" -> "1",
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "miro-transformer"
  )

  it("""
      should poll the Dynamo stream for Miro records, transform into Work
      instances, and push them into the id_minter SNS topic where those
      messages are transformable
      """) {

    val miroID = "M0000001"
    val title = "A guide for a giraffe"

    val secondMiroID = "M0000002"
    val secondTitle = "A song about a snake"

    sendMiroImageToSQS(miroID, shouldNotTransformMessage(title))
    sendMiroImageToSQS(secondMiroID, shouldTransformMessage(secondTitle))

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)

      assertSNSMessageContains(snsMessages.head,
                               secondMiroID,
                               secondTitle)
    }
  }

  private def assertSNSMessageContains(snsMessage: MessageInfo,
                                       miroID: String,
                                       imageTitle: String) = {
    val parsedWork = JsonUtil.fromJson[Work](snsMessage.message).get
    parsedWork.identifiers.head.value shouldBe miroID
    parsedWork.title shouldBe imageTitle
  }


  def shouldTransformMessage(imageTitle: String) =
    buildJSONForWork(s""""image_title": "$imageTitle"""")

  def shouldNotTransformMessage(imageTitle: String) = s"""{
          "image_title": "$imageTitle",
          "image_cleared": "N",
          "image_copyright_cleared": "N",
          "image_tech_file_size": ["100000"]
        }"""

  private def sendMiroImageToSQS(miroID: String, message: String) = {
    val miroTransformable = MiroTransformable(
      miroID,
      "Images-A",
      message)

    val sqsMessage = SQSMessage(
      Some("subject"),
      JsonUtil.toJson(miroTransformable).get,
      "topic",
      "messageType",
      "timestamp")

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)
  }

}
