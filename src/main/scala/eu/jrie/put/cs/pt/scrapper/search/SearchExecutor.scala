package eu.jrie.put.cs.pt.scrapper.search


import java.sql.Timestamp
import java.time.Instant
import java.util.UUID.randomUUID

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Sink, Source}
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesParamsTable.SearchParamRow
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesTable.SearchRow
import eu.jrie.put.cs.pt.scrapper.redis.Message.TaskMessage
import eu.jrie.put.cs.pt.scrapper.redis.Publisher
import eu.jrie.put.cs.pt.scrapper.redis.Publisher.{EndPublish, Publish}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}


object SearchExecutor {
  final case class StartSearch()
  final val SEARCH_TASKS_CHANNEL = "pt-scraper-search-tasks"

  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  def apply(redis: RedisClient): Behavior[StartSearch] = Behaviors.receive { (context, _) =>
    context.log.info("searches started")

    implicit val system: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContext = context.system.executionContext
    val publisher = context.spawn(Publisher(redis), "searchTaskPublisher")

    Await.result(
      findActiveSearches
        .runForeach { case (taskId: String, params: Map[String, String]) =>
          publisher ! Publish(SEARCH_TASKS_CHANNEL, TaskMessage(taskId, params))
        }
        .andThen(_ => publisher ! EndPublish()),
      Duration.Inf
    )

    context.log.trace("searches ended")
    Behaviors.same
  }

  def findActiveSearches(implicit system: ActorSystem[Nothing]): Source[(String, Map[String, String]), NotUsed] = {
    implicit val context: ExecutionContext = system.executionContext
    Slick.source(sql"SELECT * FROM search WHERE active = true".as[SearchRow])
//      .map { row => (row.id, SearchParams(row.brand, row.model, row.minMileage, row.maxMileage)) }
      .map { _.id }
      .map { (_, randomUUID.toString, Timestamp.from(Instant.now())) }
      .via (
        Slick.flowWithPassThrough { case (searchId: Int, taskId: String, timestamp: Timestamp) =>
          sqlu"INSERT INTO task (id, search_id, start_time) VALUES($taskId, $searchId, $timestamp)"
            .map(_ => (searchId, taskId))
        }
      )
      .map { case (searchId: Int, taskId: String) =>
        val futureParams = Slick.source(sql"SELECT * FROM search_param WHERE search_id = $searchId".as[SearchParamRow])
          .runWith(Sink.seq[SearchParamRow])
          .map(seq => seq.map(p => p.name -> p.value).toMap)
        (taskId, Await.result(futureParams, Duration.Inf))
      }
  }
}
