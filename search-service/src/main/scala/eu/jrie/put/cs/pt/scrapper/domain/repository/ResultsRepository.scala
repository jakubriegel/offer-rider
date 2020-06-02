package eu.jrie.put.cs.pt.scrapper.domain.repository

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Sink, Source}
import eu.jrie.put.cs.pt.scrapper.domain.repository.Repository.RepoMsg
import eu.jrie.put.cs.pt.scrapper.domain.repository.ResultsRepository.{AddResult, FindResults, ResultsAnswer, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.{Results, getResult}

import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ResultsRepository {
  sealed trait ResultsRepoMsg extends RepoMsg

  case class AddResult(result: Result) extends ResultsRepoMsg
  case class FindResults(userId: Int, searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]) extends ResultsRepoMsg

  case class ResultsAnswer(results: Seq[Result]) extends ResultsRepoMsg

  def apply()(implicit session: SlickSession): Behavior[ResultsRepoMsg] =
    Behaviors.setup[ResultsRepoMsg](implicit context => new ResultsRepository)
}

private class ResultsRepository(
                                 implicit context: ActorContext[ResultsRepoMsg],
                                 protected implicit val session: SlickSession
                               ) extends Repository[ResultsRepoMsg] {
  import session.profile.api._

  override def onMessage(msg: ResultsRepoMsg): Behavior[ResultsRepoMsg] = {
    msg match {
      case AddResult(result) => addNewResult(result)
      case FindResults(userId, searchId, taskId, replyTo) => findResults(userId, searchId, taskId, replyTo)
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def addNewResult(result: Result): Behavior[ResultsRepoMsg] = {
    val lastIds = Slick.source {
      sql"""
           SELECT offer_id FROM result
           WHERE task_id = (
            SELECT id FROM task
            WHERE search_id = (SELECT search_id FROM task WHERE id = ${result.taskId})
            AND id != ${result.taskId}
            ORDER BY end_time DESC LIMIT 1
          );
          """.as[String]
    } .runWith(Sink.seq)

    val action = Future { result }
      .zipWith(lastIds) { case (result, lastOfferIds) =>
        val newcomer = result.offerId
          .map { lastOfferIds.contains }
          .forall { !_ }
        (result, newcomer)
      }
      .map { case (r, newcomer) =>
        (None, r.taskId, r.offerId, r.title, r.subtitle, r.price, r.currency, r.url, r.imgUrl, newcomer)
      }
      .map { (_, TableQuery[Results]) }
      .flatMap { case (row, table) =>
        session.db.run((table returning table.map(_.id)) += row)
      }
      .map { _.get }
      .flatMap { resultId =>
        Source(result.params)
          .filter { case (_, value) => value != null }
          .runWith(Slick.sink { case (name, value) =>
            sqlu"INSERT INTO result_param VALUES ($resultId, $name, $value)"
          })
      }

    Await.ready(action, Duration.create(15, TimeUnit.SECONDS))
    Behaviors.same
  }

  private def findResults(userId: Int, searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]): Behavior[ResultsRepoMsg] = {
    Slick.source {
      taskId match {
        case Some(id) =>
          sql"""
                SELECT * FROM result
                WHERE task_id = $id
                AND task_id IN (SELECT id FROM task WHERE search_id = $searchId AND search_id IN (SELECT id FROM search WHERE user_id = $userId))
                """.as[Result]
        case None =>
          sql"""
               SELECT * FROM result
               WHERE task_id IN (SELECT id FROM task WHERE search_id = $searchId AND search_id IN (SELECT id FROM search WHERE user_id = $userId))
               """.as[Result]
      }
    }.map { r =>
      Slick.source {
        sql"SELECT * FROM result_param WHERE result_id = ${r.id.get}".as[(Int, String, String)]
      } .map { case (_, name, value) => (name, value) }
        .runWith(Sink.seq)
        .map { _.sortWith(_._1 > _._2) }
        .map { ListMap.newBuilder.addAll(_).result }
        .map {
          Result(r.id, r.taskId, r.offerId, r.title, r.subtitle, r.price, r.currency, r.url, r.imgUrl, r.newcomer, _)
        }
    }
      .runWith(Sink.seq)
      .flatMap { a => Future.sequence(a) }
      .map { ResultsAnswer }
      .andThen { replyTo ! _.get }

    Behaviors.same
  }
}