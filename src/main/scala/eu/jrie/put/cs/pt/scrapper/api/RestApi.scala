package eu.jrie.put.cs.pt.scrapper.api

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
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.model.Search
import eu.jrie.put.cs.pt.scrapper.search.SearchRepository
import eu.jrie.put.cs.pt.scrapper.search.SearchRepository.{GetSearches, SearchRepoMsg, SearchesAnswer}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object RestApi {

  case class SearchesMessage(userId: Long, searches: Seq[Search])

  private def routes(implicit actorSystem: ActorSystem[_], searchesRepo: ActorRef[SearchRepoMsg]): Route = {

    import akka.actor.typed.scaladsl.AskPattern._
    implicit val timeout: Timeout = 15.seconds
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    val mapper = new ObjectMapper().registerModule(new DefaultScalaModule)
    path("search") {
      get {
        parameters(Symbol("userId").as[Int], Symbol("active").as[Boolean].?) { (userId, active) =>
          val data: Future[SearchesAnswer] = searchesRepo ? (GetSearches(userId, active, _))
          complete(
            data.map { _.searches }
              .flatMap { _.runWith(Sink.seq) }
              .map { SearchesMessage(userId, _) }
              .map { mapper.writeValueAsString(_) }
              .map { HttpEntity(ContentTypes.`application/json`, _) }
          )
        }
      }
    }
  }


  def run: ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>

    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    val config = ConfigFactory.load().getConfig("service.api")
    val db = ctx.spawn(SearchRepository(), "searchRepoAPI")

    Http().bindAndHandle(routes(ctx.system, db), config.getString("host"), config.getInt("port"))
      .onComplete {
        case Success(bound) =>
          ctx.log.info(s"Api online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
        case Failure(e) =>
          Console.err.println(s"Server could not start!")
          e.printStackTrace()
          ctx.self ! Done
      }
    Behaviors.receiveMessage {
      case Done =>
        Behaviors.stopped
    }
  }, "apiSystem")
}
