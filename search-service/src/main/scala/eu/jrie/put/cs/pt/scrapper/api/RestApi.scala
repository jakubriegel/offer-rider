package eu.jrie.put.cs.pt.scrapper.api

import java.time.Instant

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.{FindResults, ResultsAnswer, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.domain.results.{Result, ResultsRepository}
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchRepository._
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator.{CreateForSearch, SearchTaskCreatorMsg}
import eu.jrie.put.cs.pt.scrapper.domain.search.{Search, SearchRepository}
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository.{FindTasks, TasksRepoMsg, TasksResponse}
import eu.jrie.put.cs.pt.scrapper.domain.tasks.{Task, TasksRepository}
import eu.jrie.put.cs.pt.scrapper.infra.json.Mapper

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object RestApi {

  case class SearchesMessage(userId: Long, searches: Seq[Search])
  case class TasksMessage(userId: Long, searchId: Int, tasks: Seq[Task])
  case class ResultsMessage(userId: Long, searchId: Int, taskId: Option[String], query: Option[String], results: Seq[Result], date: Instant)

  private def routes(
                      implicit actorSystem: ActorSystem[_],
                      searchesRepo: ActorRef[SearchRepoMsg],
                      tasksRepo: ActorRef[TasksRepoMsg],
                      resultsRepo: ActorRef[ResultsRepoMsg],
                      tasksCreator: ActorRef[SearchTaskCreatorMsg]
                    ): Route = {

    import akka.actor.typed.scaladsl.AskPattern._
    implicit val timeout: Timeout = 15.seconds
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    val mapper = Mapper()

    implicit def parseRequest(search: String): Search = { mapper.readValue(search, classOf[Search]) }
    implicit def parseResponse(search: Search): String = { mapper.writeValueAsString(search) }

    def searchResponse(answer: Future[SearchAnswer], status: StatusCode = OK, triggerTask: Boolean = true) =
      answer.flatMap { _.search }
        .andThen { s => if (triggerTask) tasksCreator ! CreateForSearch(s.get) }
        .map { HttpEntity(ContentTypes.`application/json`, _) }
        .map { HttpResponse(status, Seq.empty, _) }

    import eu.jrie.put.cs.pt.scrapper.api.Cors.publicPath
    val searchRoutes = publicPath("search")(
      concat(
        get {
          parameters(Symbol("userId").as[Int], Symbol("active").as[Boolean].?) { (userId, active) =>
            val data: Future[SearchesAnswer] = searchesRepo ? (FindSearches(userId, active, _))
            complete(
              data.map { _.searches }
                .flatMap { _.runWith(Sink.seq) }
                .map { SearchesMessage(userId, _) }
                .map { mapper.writeValueAsString }
                .map { HttpEntity(ContentTypes.`application/json`, _) }
                .map { HttpResponse(OK, Seq.empty, _) }
            )
          }
        },
        post {
          entity(as[String]) { request =>
            val created: Future[SearchAnswer] = searchesRepo ? (AddSearch(request, _))
            completeOrRecoverWith(searchResponse(created, Created)) { case e: InvalidParamException =>
              complete(
                HttpResponse(
                  UnprocessableEntity,
                  Seq.empty,
                  HttpEntity(ContentTypes.`text/plain(UTF-8)`, e.asInstanceOf[Exception].getMessage)
                )
              )
            }
          }
        },
        put {
          pathPrefix(IntNumber) { searchId =>
            concat(
              path("activate") {
                complete(searchResponse(searchesRepo ? (ActivateSearch(searchId, _))))
              },
              path("deactivate") {
                complete(searchResponse(searchesRepo ? (DeactivateSearch(searchId, _)), triggerTask = false))
              }
            )
          }
        }
      )
    )

    val tasksRoutes = publicPath("tasks") {
      get {
        parameters(Symbol("userId").as[Int], Symbol("searchId").as[Int]) { (userId, searchId) =>
          val tasks: Future[TasksResponse] = tasksRepo ? (FindTasks(userId, searchId, _))
          complete(
            tasks.map { _.tasks }
              .map { TasksMessage(userId, searchId, _) }
              .map { mapper.writeValueAsString }
              .map { HttpEntity(ContentTypes.`application/json`, _) }
              .map { HttpResponse(OK, Seq.empty, _) }
          )
        }
      }
    }

    val resultsRoutes = publicPath("results") {
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
                .map { HttpResponse(OK, Seq.empty, _) }
            )
          }
        }
      )
    }

    concat(searchRoutes, tasksRoutes, resultsRoutes)
  }


  def run(tasksCreator: ActorRef[SearchTaskCreatorMsg])(implicit session: SlickSession): ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>

    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    val config = ConfigFactory.load().getConfig("service.api")
    val searchesRepo = ctx.spawn(SearchRepository(), "searchRepoAPI")
    val tasksRepo = ctx.spawn(TasksRepository(), "tasksRepoAPI")
    val resultsRepo = ctx.spawn(ResultsRepository()(session, null), "resultRepoAPI")

    Http().bindAndHandle(
      routes(ctx.system, searchesRepo, tasksRepo, resultsRepo, tasksCreator: ActorRef[SearchTaskCreatorMsg]),
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
