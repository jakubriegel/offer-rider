package eu.jrie.put.cs.pt.scrapper.domain.search


import java.time.Instant
import java.util.UUID.randomUUID

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.Source
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.domain.repository.SearchRepository.{EndSearchRepo, FindActiveSearches}
import eu.jrie.put.cs.pt.scrapper.domain.repository.TasksRepository._
import eu.jrie.put.cs.pt.scrapper.domain.repository.{SearchRepository, TasksRepository}
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator.SearchTaskCreatorMsg
import eu.jrie.put.cs.pt.scrapper.model.{Search, Task}
import eu.jrie.put.cs.pt.scrapper.redis.Message.TaskMessage
import eu.jrie.put.cs.pt.scrapper.redis.Publisher
import eu.jrie.put.cs.pt.scrapper.redis.Publisher.{EndPublish, Publish}

import scala.concurrent.Future.failed
import scala.concurrent.{Await, ExecutionContext, Future}


object SearchTaskCreator {
  sealed trait SearchTaskCreatorMsg
  final case class CreateForAllActive(replyTo: ActorRef[Done]) extends SearchTaskCreatorMsg
  final case class CreateForSearch(search: Search) extends SearchTaskCreatorMsg
  final val SEARCH_TASKS_CHANNEL = "pt-scraper-search-tasks"

  def apply(redis: RedisClient)(implicit session: SlickSession): Behavior[SearchTaskCreatorMsg] =
    Behaviors.setup[SearchTaskCreatorMsg](implicit context => new SearchTaskCreator(redis))

}

private class SearchTaskCreator (redis: RedisClient)
                                (
                                  implicit ctx: ActorContext[SearchTaskCreatorMsg],
                                  session: SlickSession
                                ) extends AbstractBehavior[SearchTaskCreatorMsg](ctx) {
  import akka.actor.typed.scaladsl.AskPattern._
  import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator.{CreateForAllActive, CreateForSearch, SEARCH_TASKS_CHANNEL}

  import scala.concurrent.duration._

  private implicit val system: ActorSystem[Nothing] = ctx.system
  private implicit val executionContext: ExecutionContext = ctx.system.executionContext
  private implicit val timeout: Timeout = 15.seconds

  private val publisher = ctx.spawn(Publisher(redis), "publisherSearchTaskCreator")
  private val searchRepo = ctx.spawn(SearchRepository(), "searchRepositorySearchTaskCreator")
  private val tasksRepo = ctx.spawn(TasksRepository(), "tasksRepositorySearchTaskCreator")

  override def onMessage(msg: SearchTaskCreatorMsg): Behavior[SearchTaskCreatorMsg] = msg match {
    case CreateForAllActive(replyTo) =>
      createForAllActive()
      replyTo ! Done
      Behaviors.same
    case CreateForSearch(search) =>
      createForSearch(search)
      Behaviors.same
    case _ =>
      ctx.log.error("Received invalid msg")
      publisher ! EndPublish()
      searchRepo ! EndSearchRepo()
      tasksRepo ! EndTasksRepo()
      Behaviors.stopped
  }

  private def createForAllActive(): Unit = {
    ctx.log.info("searches task creation started")
    Await.result(
      (tasksRepo ? CheckForNotEndedTasks)
        .map { _.has }
        .flatMap { has =>
          if (!has) searchRepo ? FindActiveSearches
          else failed(new Exception("Creation stated before previous tasks ended. Aborting..."))
        }
        .map { _.searches }
        .map { publishTasks },
      Duration.Inf
    ).andThen { result =>
      if (result.isFailure) ctx.log.error(result.failed.get.getMessage)
    }

    ctx.log.debug("searches task creation ended")
  }

  private def createForSearch(search: Search): Unit = {
    ctx.log.info(s"Creating search tasks for ${search.id.get}")
    Await.ready(
      Future { Source.single(search) }
        .map { publishTasks },
      Duration.Inf
    )
  }

  private def publishTasks(searches: Source[Search, NotUsed]): Future[Done] = {
    searches.map { s => (s.id.get, s.params, randomUUID.toString, Instant.now()) }
      .map { case (searchId, params, taskId, start) =>
        (Task(taskId, searchId, start, None), params)
      }
      .mapAsync(1) { case (task, params) =>
        val addedId: Future[TaskResponse] = tasksRepo ? (AddTask(task, _))
        addedId.map { _.id }
          .map { (_, params) }
      }
      .runForeach { case (taskId: String, params: Map[String, String]) =>
        publisher ! Publish(SEARCH_TASKS_CHANNEL, TaskMessage(taskId, params))
      }
  }
}
