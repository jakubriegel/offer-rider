package eu.jrie.put.cs.pt.scrapper.api

import java.time.Instant

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.domain.repository.ResultsRepository.{FindResults, ResultsAnswer, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.domain.repository.SearchRepository.{AddSearch, FindSearches, SearchAnswer, SearchRepoMsg, SearchesAnswer}
import eu.jrie.put.cs.pt.scrapper.domain.repository.{ResultsRepository, SearchRepository}
import eu.jrie.put.cs.pt.scrapper.model.{Result, Search}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object RestApi {

  case class SearchesMessage(userId: Long, searches: Seq[Search])
  case class ResultsMessage(userId: Long, searchId: Int, taskId: Option[String], query: Option[String], results: Seq[Result], date: Instant)

  private def routes(
                      implicit actorSystem: ActorSystem[_],
                      searchesRepo: ActorRef[SearchRepoMsg],
                      resultsRepo: ActorRef[ResultsRepoMsg]
                    ): Route = {

    import akka.actor.typed.scaladsl.AskPattern._
    implicit val timeout: Timeout = 15.seconds
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    val mapper = new ObjectMapper()
      .registerModule(new DefaultScalaModule)
      .registerModule(new JavaTimeModule)
      .registerModule(new Jdk8Module)
    implicit def parseRequest(search: String): Search = { mapper.readValue(search, classOf[Search]) }
    implicit def parseResponse(search: Search): String = { mapper.writeValueAsString(search) }

    val searchRoutes = path("search") {
      concat(
        get {
          parameters(Symbol("userId").as[Int], Symbol("active").as[Boolean].?) { (userId, active) =>
            val data: Future[SearchesAnswer] = searchesRepo ? (FindSearches(userId, active, _))
            complete(
              data.map { _.searches }
                .flatMap { _.runWith(Sink.seq) }
                .map { SearchesMessage(userId, _) }
                .map { mapper.writeValueAsString(_) }
                .map { HttpEntity(ContentTypes.`application/json`, _) }
                .map { HttpResponse(StatusCodes.OK, Seq.empty, _) }
            )
          }
        },
        post {
          entity(as[String]) { request =>
            val created: Future[SearchAnswer] = searchesRepo ? (AddSearch(request, _))
            complete(
              created.map { _.search }
                .map { HttpEntity(ContentTypes.`application/json`, _) }
                .map { HttpResponse(StatusCodes.Created, Seq.empty, _) }
            )
          }
        }
      )
    }

    val resultsRoutes = path("results") {
      concat(
        get {
          parameters(
            Symbol("userId").as[Int], Symbol("searchId").as[Int], Symbol("taskId").as[String].?, Symbol("query").as[String].?
          ) { (userId, searchId, taskId, query) =>
            val data: Future[ResultsAnswer] = resultsRepo ? (FindResults(userId, searchId, taskId, _))
            complete(
              data.map { _.results }
                .map { ResultsMessage(userId, searchId, taskId, query, _, Instant.now) }
                .map { mapper.writeValueAsString }
                .map { HttpEntity(ContentTypes.`application/json`, _) }
                .map { HttpResponse(StatusCodes.OK, Seq.empty, _) }
            )
          }
        }
      )
    }

    concat(searchRoutes, resultsRoutes)
  }


  def run: ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>

    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    val config = ConfigFactory.load().getConfig("service.api")
    val searchesRepo = ctx.spawn(SearchRepository(), "searchRepoAPI")
    val resultsRepo = ctx.spawn(ResultsRepository(), "resultRepoAPI")

    Http().bindAndHandle(
      routes(ctx.system, searchesRepo, resultsRepo),
      config.getString("host"), config.getInt("port")
    ).onComplete {
      case Success(bound) =>
        ctx.log.info(s"Api online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        Console.err.println(s"Server could not start!")
        e.printStackTrace()
        ctx.self ! Done
    }

    Behaviors.receiveMessage {
      case Done => Behaviors.stopped
    }
  }, "apiSystem")
}
