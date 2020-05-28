package eu.jrie.put.cs.pt.scrapper.domain.repository

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.domain.repository.Repository.RepoMsg
import eu.jrie.put.cs.pt.scrapper.domain.repository.SearchRepository._
import eu.jrie.put.cs.pt.scrapper.model.Search
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesTable.Searches
import slick.jdbc.SQLActionBuilder

import scala.collection.immutable.ListMap
import scala.concurrent.Future.failed
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object SearchRepository {
  sealed trait SearchRepoMsg extends RepoMsg
  case class AddSearch(search: Search, replyTo: ActorRef[SearchAnswer]) extends SearchRepoMsg
  case class FindSearches(userId: Int, active: Option[Boolean], replyTo: ActorRef[SearchesAnswer]) extends SearchRepoMsg
  case class FindActiveSearches(replyTo: ActorRef[SearchesAnswer]) extends SearchRepoMsg
  case class ActivateSearch(searchId: Int, replyTo: ActorRef[SearchAnswer]) extends SearchRepoMsg
  case class DeactivateSearch(searchId: Int, replyTo: ActorRef[SearchAnswer]) extends SearchRepoMsg

  case class SearchAnswer(search: Future[Search]) extends SearchRepoMsg
  case class SearchesAnswer(searches: Source[Search, NotUsed]) extends SearchRepoMsg

  case class EndSearchRepo() extends SearchRepoMsg

  private type SearchRow = (Int, Int, Boolean)

  private val allowedParams = ConfigFactory.load().getStringList("service.search.allowedParams")

  case class InvalidParamException(param: String) extends Exception(
    s"Invalid param: $param. Allowed params are: $allowedParams"
  )

  def apply()(implicit session: SlickSession): Behavior[SearchRepoMsg] =
    Behaviors.setup(implicit context => new SearchRepository)
}

private class SearchRepository(
                                implicit context: ActorContext[SearchRepoMsg],
                                protected implicit val session: SlickSession
                              ) extends Repository[SearchRepoMsg] {
  import session.profile.api._

  override def onMessage(msg: SearchRepoMsg): Behavior[SearchRepoMsg] = {
    msg match {
      case AddSearch(search, replyTo) => addSearch(search, replyTo)
      case FindSearches(userId, onlyActive, replyTo) =>
        val filtered = Slick.source(sql"SELECT * FROM search WHERE user_id = $userId".as[SearchRow])
          .filter { case (_, _, active) => onlyActive forall (active == _) }
        replyTo ! SearchesAnswer(findSearches(filtered))
        Behaviors.same
      case FindActiveSearches(replyTo) =>
        replyTo ! SearchesAnswer(findSearches(sql"SELECT * FROM search WHERE active = true"))
        Behaviors.same
      case ActivateSearch(searchId, replyTo) =>
        replyTo ! SearchAnswer(
          activate(searchId)
            .map { _ => findSearches(sql"SELECT * FROM search WHERE id = $searchId") }
            .flatMap { _.runWith(Sink.head) }
        )
        Behaviors.same
      case DeactivateSearch(searchId, replyTo) =>
        replyTo ! SearchAnswer(
          deactivate(searchId)
            .map { _ => findSearches(sql"SELECT * FROM search WHERE id = $searchId") }
            .flatMap { _.runWith(Sink.head) }
        )
        Behaviors.same
      case EndSearchRepo() =>
        Behaviors.stopped
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def addSearch(search: Search, replyTo: ActorRef[SearchAnswer]): Behavior[SearchRepoMsg] = {
    val unwantedParam = search.params
      .map { case (param, _) => param }
      .filterNot { allowedParams.contains }
      .lastOption

    if (unwantedParam.nonEmpty) {
      replyTo ! SearchAnswer(failed(InvalidParamException(unwantedParam.get)))
    }
    else {
      val added: Future[Search] = Future { (None, search.userId, true) }
        .map { (_, TableQuery[Searches]) }
        .flatMap { case (row, table) =>
          session.db.run((table returning table.map(_.id)) += row)
        }
        .map { _.get }
        .flatMap { searchId =>
          Source(search.params)
            .filter { case (_, value) => value != null }
            .runWith(Slick.sink { case (name, value) =>
              sqlu"INSERT INTO search_param VALUES ($searchId, $name, $value)"
            }).map { _ => searchId }
        }
        .map { searchId => findSearches(sql"SELECT * FROM search WHERE id = $searchId") }
        .flatMap { _.runWith(Sink.head) }
      replyTo ! SearchAnswer(added)
    }

    Behaviors.same
  }

  private def findSearches(sql: SQLActionBuilder)(implicit system: ActorSystem[_]): Source[Search, NotUsed] =  {
    findSearches(Slick.source(sql.as[SearchRow]))
  }

  private def findSearches(source: Source[SearchRow, NotUsed])(implicit system: ActorSystem[_]): Source[Search, NotUsed] = {
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    source
      .map { case (id, userId, active) =>
        val futureParams = Slick.source {
          sql"SELECT * FROM search_param WHERE search_id = $id".as[(Int, String, String)]
        } .map { case (_, name, value) => (name, value) }
          .runWith(Sink.seq)
          .map { _.sortWith(_._1 > _._2) }
          .map { ListMap.newBuilder.addAll(_).result }
        ((id, userId, active), Await.result(futureParams, Duration.Inf))
      }
      .map { case ((id, userId, active), params) =>
        Search(Option(id), userId, params, active)
      }
  }

  private def activate(searchId: Int) = Source.single { searchId }
      .runWith(Slick.sink { id => sqlu"UPDATE search SET active = true WHERE id = $id" })

  private def deactivate(searchId: Int) = Source.single { searchId }
      .runWith(Slick.sink { id => sqlu"UPDATE search SET active = false WHERE id = $id" })
}
