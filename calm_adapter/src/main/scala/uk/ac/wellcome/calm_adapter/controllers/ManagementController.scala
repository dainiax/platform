package uk.ac.wellcome.platform.calm_adapter.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.twitter.inject.annotations.Flag

@Singleton
class ManagementController @Inject()() extends Controller {
  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }
}