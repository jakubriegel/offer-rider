package eu.jrie.put.cs.pt.scrapper.domain.search


import java.sql.Timestamp
import java.time.Instant
import java.util.UUID.randomUUID

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchRepository.{EndSearchRepo, GetActiveSearches, SearchRepoMsg, SearchesAnswer}
import eu.jrie.put.cs.pt.scrapper.redis.Message.TaskMessage
import eu.jrie.put.cs.pt.scrapper.redis.Publisher
import eu.jrie.put.cs.pt.scrapper.redis.Publisher.{EndPublish, Publish}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


object SearchExecutor {
  final case class StartSearch()
  final val SEARCH_TASKS_CHANNEL = "pt-scraper-search-tasks"

  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  def apply(redis: RedisClient): Behavior[StartSearch] = Behaviors.receive { (ctx, _) =>
    ctx.log.info("searches task creation started")

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val executionContext: ExecutionContext = ctx.system.executionContext

    val publisher = ctx.spawn(Publisher(redis), "searchTaskPublisher")
    val searchRepo = ctx.spawn(SearchRepository(), "searchRepositorySearchExecutor")

    Await.result(
      tasks(searchRepo)
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

  def tasks(searchesRepo: ActorRef[SearchRepoMsg])(implicit system: ActorSystem[_]): Future[Source[(String, Map[String, String]), NotUsed]] = {
    import akka.actor.typed.scaladsl.AskPattern._

    import scala.concurrent.duration._

    implicit val context: ExecutionContext = system.executionContext
    implicit val timeout: Timeout = 15.seconds


    val searches: Future[SearchesAnswer] = searchesRepo ? GetActiveSearches
    searches.map { _.searches }
      .map { source =>
        source.map { s => (s.id.get, s.params, randomUUID.toString, Timestamp.from(Instant.now())) }
        .via (
          Slick.flowWithPassThrough { case (searchId: Int, params: Map[String, String], taskId: String, timestamp: Timestamp) =>
            sqlu"INSERT INTO task (id, search_id, start_time) VALUES($taskId, $searchId, $timestamp)"
              .map(_ => (taskId, params))
          }
        )
      }
  }
}
