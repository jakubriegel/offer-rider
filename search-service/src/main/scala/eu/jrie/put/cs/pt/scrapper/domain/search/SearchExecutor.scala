package eu.jrie.put.cs.pt.scrapper.domain.search


import java.time.Instant
import java.util.UUID.randomUUID

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.domain.repository.SearchRepository.{EndSearchRepo, FindActiveSearches, SearchRepoMsg, SearchesAnswer}
import eu.jrie.put.cs.pt.scrapper.domain.repository.TasksRepository.{AddTask, TaskResponse, TasksRepoMsg}
import eu.jrie.put.cs.pt.scrapper.domain.repository.{SearchRepository, TasksRepository}
import eu.jrie.put.cs.pt.scrapper.model.Task
import eu.jrie.put.cs.pt.scrapper.redis.Message.TaskMessage
import eu.jrie.put.cs.pt.scrapper.redis.Publisher
import eu.jrie.put.cs.pt.scrapper.redis.Publisher.{EndPublish, Publish}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


object SearchExecutor {
  final case class StartSearch()
  final val SEARCH_TASKS_CHANNEL = "pt-scraper-search-tasks"

  def apply(redis: RedisClient)(implicit session: SlickSession): Behavior[StartSearch] = Behaviors.receive { (ctx, _) =>
    ctx.log.info("searches task creation started")

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val executionContext: ExecutionContext = ctx.system.executionContext

    val publisher = ctx.spawn(Publisher(redis), "searchTaskPublisher")
    val searchRepo = ctx.spawn(SearchRepository(), "searchRepositorySearchExecutor")
    val tasksRepo = ctx.spawn(TasksRepository(), "tasksRepositorySearchExecutor")

    Await.result(
      tasks(searchRepo, tasksRepo)
        .map { source =>
          source.runForeach { case (taskId: String, params: Map[String, String]) =>
            publisher ! Publish(SEARCH_TASKS_CHANNEL, TaskMessage(taskId, params))
          }.andThen(_ => {
            publisher ! EndPublish()
            searchRepo ! EndSearchRepo()
          })
        },
      Duration.Inf
    )

    ctx.log.debug("searches task creation ended")
    Behaviors.same
  }

  def tasks(searchesRepo: ActorRef[SearchRepoMsg], tasksRepo: ActorRef[TasksRepoMsg])(implicit system: ActorSystem[_]): Future[Source[(String, Map[String, String]), NotUsed]] = {
    import akka.actor.typed.scaladsl.AskPattern._

    import scala.concurrent.duration._

    implicit val context: ExecutionContext = system.executionContext
    implicit val timeout: Timeout = 15.seconds


    val searches: Future[SearchesAnswer] = searchesRepo ? FindActiveSearches
    searches.map { _.searches }
      .map { source =>
        source.map { s => (s.id.get, s.params, randomUUID.toString, Instant.now()) }
          .map { case (searchId: Int, params: Map[String, String], taskId: String, start: Instant) =>
            (Task(taskId, searchId, start, None), params)
          }
          .mapAsync(1) { case (task: Task, params: Map[String, String]) =>
            val addedId: Future[TaskResponse] = tasksRepo ? (AddTask(task, _))
            addedId.map { _.id }
              .map { (_, params) }
          }
      }
  }
}
