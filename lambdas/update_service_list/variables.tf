variable "lambda_error_alarm_arn" {}
variable "every_minute_arn" {}
variable "every_minute_name" {}
variable "iam_policy_document_describe_services_json" {}
variable "bucket_dashboard_id" {}
variable "bucket_dashboard_arn" {}

variable "dashboard_assumable_roles" {
  type = "list"
}
