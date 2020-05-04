package eu.jrie.put.cs.pt.scrapper.api

import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route

object Cors {

  import akka.http.scaladsl.model.HttpMethods._
  import akka.http.scaladsl.server.Directives._

  private val corsResponseHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Headers`("Content-Type", "Origin"),
  )

  private def preflightRequestRoute: Route = options {
    complete(HttpResponse(StatusCodes.OK).
      withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  def publicPath(url: String)(routes: Route*): Route = {
    path(url) {
      respondWithDefaultHeaders(corsResponseHeaders) {
        concat(preflightRequestRoute :: routes.toList:_*)
      }
    }
  }
}
