package eu.jrie.put.cs.pt.scrapper.domain.search


import java.time.Instant
import java.util.UUID.randomUUID

import akka.NotUsed
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.domain.repository.SearchRepository.{EndSearchRepo, FindActiveSearches, SearchRepoMsg, SearchesAnswer}
import eu.jrie.put.cs.pt.scrapper.domain.repository.TasksRepository.{AddTask, EndTasksRepo, TaskResponse, TasksRepoMsg}
import eu.jrie.put.cs.pt.scrapper.domain.repository.{SearchRepository, TasksRepository}
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator.SearchTaskCreatorMsg
import eu.jrie.put.cs.pt.scrapper.model.Task
import eu.jrie.put.cs.pt.scrapper.redis.Message.TaskMessage
import eu.jrie.put.cs.pt.scrapper.redis.Publisher
import eu.jrie.put.cs.pt.scrapper.redis.Publisher.{EndPublish, Publish}

import scala.concurrent.{Await, ExecutionContext, Future}


object SearchTaskCreator {
  sealed trait SearchTaskCreatorMsg
  final case class CreateForAllActive() extends SearchTaskCreatorMsg
  final case class CreateForSearch(searchId: Int) extends SearchTaskCreatorMsg
  final val SEARCH_TASKS_CHANNEL = "pt-scraper-search-tasks"

  def apply(redis: RedisClient)(implicit session: SlickSession): Behavior[SearchTaskCreatorMsg] =
    Behaviors.setup[SearchTaskCreatorMsg](implicit context => new SearchTaskCreator(redis))

}

private class SearchTaskCreator (redis: RedisClient)
                                (
                                  implicit ctx: ActorContext[SearchTaskCreatorMsg],
                                  session: SlickSession
                                ) extends AbstractBehavior[SearchTaskCreatorMsg](ctx) {
  import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator.{CreateForAllActive, CreateForSearch, SEARCH_TASKS_CHANNEL}

  import scala.concurrent.duration._

  private implicit val system: ActorSystem[Nothing] = ctx.system
  private implicit val executionContext: ExecutionContext = ctx.system.executionContext
  private implicit val timeout: Timeout = 15.seconds

  override def onMessage(msg: SearchTaskCreatorMsg): Behavior[SearchTaskCreatorMsg] = msg match {
    case CreateForAllActive() =>
      createForAllActive()
      Behaviors.same
    case CreateForSearch(searchId) =>
      Behaviors.same
    case _ =>
      ctx.log.error("Received invalid msg")
      Behaviors.stopped
  }

  private def createForAllActive(): Unit = {
    ctx.log.info("searches task creation started")

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
            tasksRepo ! EndTasksRepo()
          })
        },
      Duration.Inf
    )

    ctx.log.debug("searches task creation ended")
  }

  def tasks(searchesRepo: ActorRef[SearchRepoMsg], tasksRepo: ActorRef[TasksRepoMsg]): Future[Source[(String, Map[String, String]), NotUsed]] = {
    import akka.actor.typed.scaladsl.AskPattern._

    val searches: Future[SearchesAnswer] = searchesRepo ? FindActiveSearches
    searches.map { _.searches }
      .map { source =>
        source.map { s =>
            (s.id.get, s.params, randomUUID.toString, Instant.now())
          }
          .map { case (searchId, params, taskId, start) =>
            (Task(taskId, searchId, start, None), params)
          }
          .mapAsync(1) { case (task, params) =>
            val addedId: Future[TaskResponse] = tasksRepo ? (AddTask(task, _))
            addedId.map { _.id }
              .map { (_, params) }
          }
      }
  }
}
