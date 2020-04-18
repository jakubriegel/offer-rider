package eu.jrie.put.cs.pt.scrapper.domain.repository

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Sink, Source}
import eu.jrie.put.cs.pt.scrapper.model.Search
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesParamsTable.{SearchParamRow, SearchesParams}
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesTable.{SearchRow, Searches}
import slick.jdbc.SQLActionBuilder

import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object SearchRepository {

  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  sealed trait SearchRepoMsg
  case class AddSearch(search: Search, replyTo: ActorRef[SearchAnswer]) extends SearchRepoMsg
  case class FindSearches(userId: Int, active: Option[Boolean], replyTo: ActorRef[SearchesAnswer]) extends SearchRepoMsg
  case class FindActiveSearches(replyTo: ActorRef[SearchesAnswer]) extends SearchRepoMsg

  case class SearchAnswer(search: Search) extends SearchRepoMsg
  case class SearchesAnswer(searches: Source[Search, NotUsed]) extends SearchRepoMsg

  case class EndSearchRepo() extends SearchRepoMsg

  def apply(): Behavior[SearchRepoMsg] = Behaviors.receive { (ctx, msg) =>
    implicit val system: ActorSystem[_] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = ctx.executionContext
    msg match {
      case AddSearch(search, replyTo) =>
        Future {
          (None, search.userId, true)
        }.map { (_, TableQuery[Searches]) }
          .flatMap { case (row, table) =>
            session.db.run((table returning table.map(_.id)) += row)
          }
          .map { _.get }
          .map { (_, TableQuery[SearchesParams]) }
          .flatMap { case (searchId, table) =>
            Future.sequence(
              search.params.map { case (name, value) =>
                session.db.run(table += (searchId, name, value))
              }
            ).map { (searchId, _) }
          }
          .map { case (searchId, _) =>
            findSearches(sql"SELECT * FROM search WHERE id = $searchId").map { SearchAnswer }
          }
          .flatMap { _.runWith(Sink.head) }
          .andThen { replyTo ! _.get }

        Behaviors.same
      case FindSearches(userId, active, replyTo) =>
        val filtered = Slick.source(sql"SELECT * FROM search WHERE user_id = $userId".as[SearchRow])
          .filter { s => active forall (s.active == _) }
        replyTo ! SearchesAnswer(findSearches(filtered))
        Behaviors.same
      case FindActiveSearches(replyTo) =>
        replyTo ! SearchesAnswer(findSearches(sql"SELECT * FROM search WHERE active = true"))
        Behaviors.same
      case EndSearchRepo() =>
        Behaviors.stopped
      case _ =>
        ctx.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def findSearches(sql: SQLActionBuilder)(implicit system: ActorSystem[_]): Source[Search, NotUsed] =  {
    findSearches(Slick.source(sql.as[SearchRow]))
  }

  private def findSearches(source: Source[SearchRow, NotUsed])(implicit system: ActorSystem[_]): Source[Search, NotUsed] = {
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    source
      .map { searchRow =>
        val futureParams = Slick.source(sql"SELECT * FROM search_param WHERE search_id = ${searchRow.id}".as[SearchParamRow])
          .runWith(Sink.seq[SearchParamRow])
          .map { _.map(p => p.name -> p.value) }
          .map { _.sortWith(_._1 > _._2) }
          .map { ListMap.newBuilder.addAll(_).result }
        (searchRow, Await.result(futureParams, Duration.Inf))
      }
      .map { case (searchRow, params) =>
        Search(Option(searchRow.id), searchRow.userId, params, searchRow.active)
      }
  }
}
