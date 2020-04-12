package eu.jrie.put.cs.pt.scrapper.search

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.Source
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.SearchesTable.SearchRow
import eu.jrie.put.cs.pt.scrapper.search.SearchExecutor.session

object SearchRepository {

  import session.profile.api._

  sealed trait SearchRepoMsg
  case class AddSearch(replyTo: ActorRef[SearchRepoMsg]) extends SearchRepoMsg
  case class GetSearches(userId: Int, active: Option[Boolean], replyTo: ActorRef[SearchesAnswer]) extends SearchRepoMsg
  case class GetActiveSearches(replyTo: ActorRef[SearchRepoMsg]) extends SearchRepoMsg
  case class SearchesAnswer(searches: Source[SearchRow, NotUsed]) extends SearchRepoMsg

  def apply(): Behavior[SearchRepoMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case GetSearches(userId, active, replyTo) =>
        val searches = Slick.source(sql"SELECT * FROM search WHERE user_id = $userId".as[SearchRow])
          .filter { s => active forall (s.active == _) }
        replyTo ! SearchesAnswer(searches)
      case _ =>
        ctx.log.info("unsupported repo msg")
    }

    Behaviors.same
  }
}
