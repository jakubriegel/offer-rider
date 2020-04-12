package eu.jrie.put.cs.pt.scrapper.search

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Sink, Source}
import eu.jrie.put.cs.pt.scrapper.model.Search
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesParamsTable.SearchParamRow
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesTable.SearchRow

import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}

object SearchRepository {

  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  sealed trait SearchRepoMsg
  case class AddSearch(replyTo: ActorRef[SearchRepoMsg]) extends SearchRepoMsg
  case class GetSearches(userId: Int, active: Option[Boolean], replyTo: ActorRef[SearchesAnswer]) extends SearchRepoMsg
  case class GetActiveSearches(replyTo: ActorRef[SearchRepoMsg]) extends SearchRepoMsg
  case class SearchesAnswer(searches: Source[Search, NotUsed]) extends SearchRepoMsg

  def apply(): Behavior[SearchRepoMsg] = Behaviors.receive { (ctx, msg) =>
    implicit val system: ActorSystem[_] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = ctx.executionContext
    msg match {
      case GetSearches(userId, active, replyTo) =>
        val searches = Slick.source(sql"SELECT * FROM search WHERE user_id = $userId".as[SearchRow])
          .filter { s => active forall (s.active == _) }
          .map { searchRow =>
            val futureParams = Slick.source(sql"SELECT * FROM search_param WHERE search_id = ${searchRow.id}".as[SearchParamRow])
              .runWith(Sink.seq[SearchParamRow])
              .map { _.map(p => p.name -> p.value) }
              .map { _.sortWith(_._1 > _._2) }
              .map { ListMap.newBuilder.addAll(_).result }
            (searchRow, Await.result(futureParams, Duration.Inf))
          }
          .map { case (searchRow, params) =>
            Search(searchRow.id, searchRow.userId, params, searchRow.active)
          }
        replyTo ! SearchesAnswer(searches)
      case _ =>
        ctx.log.info("unsupported repo msg")
    }

    Behaviors.same
  }
}
