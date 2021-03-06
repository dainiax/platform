# -*- encoding: utf-8 -*-

import datetime
import decimal
import json

import boto3


class EnhancedJSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime.datetime):
            return obj.isoformat()

        if isinstance(obj, decimal.Decimal):
            return str(obj)

        return json.JSONEncoder.default(self, obj)


def publish_sns_message(topic_arn, message):
    """
    Given a topic ARN and a series of key-value pairs, publish the key-value
    data to the SNS topic.
    """
    sns = boto3.client('sns')
    resp = sns.publish(
        TopicArn=topic_arn,
        MessageStructure='json',
        Message=json.dumps({
            'default': json.dumps(
                message,
                cls=EnhancedJSONEncoder,
                iterable_as_array=True
            )
        })
    )
    print(f'SNS response = {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
