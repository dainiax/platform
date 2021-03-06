module "lambda_schedule_reindexer" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "schedule_reindexer"
  description = "Schedules the reindexer based on the ReindexerTracker table"

  environment_variables = {
    SCHEDULER_TOPIC_ARN     = "${var.service_scheduler_topic_arn}"
    DYNAMO_TABLE_NAME       = "${var.dynamodb_table_miro_table_name}"
    DYNAMO_TOPIC_ARN        = "${var.dynamo_capacity_topic_arn}"
    DYNAMO_DESIRED_CAPACITY = "125"
    CLUSTER_NAME            = "${var.ecs_services_cluster_name}"
    REINDEXERS              = "${var.dynamodb_table_miro_table_name}=miro_reindexer"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_reindexer_lambda" {
  source = "../../terraform/lambda/trigger_dynamo"

  stream_arn    = "${var.dynamodb_table_reindex_tracker_stream_arn}"
  function_arn  = "${module.lambda_schedule_reindexer.arn}"
  function_role = "${module.lambda_schedule_reindexer.role_name}"
}
