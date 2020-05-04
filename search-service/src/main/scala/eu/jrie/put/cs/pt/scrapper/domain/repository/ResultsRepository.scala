package eu.jrie.put.cs.pt.scrapper.domain.repository

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Sink
import eu.jrie.put.cs.pt.scrapper.domain.repository.Repository.RepoMsg
import eu.jrie.put.cs.pt.scrapper.domain.repository.ResultsRepository.{AddResult, FindResults, ResultsAnswer, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsParamsTable.{ResultParamRow, ResultParams}
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.{ResultRow, Results}

import scala.collection.immutable.ListMap
import scala.concurrent.Future

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
    Future {
      (None, result.taskId, result.title, result.subtitle, result.price, result.currency, result.url, result.imgUrl)
    }.map { (_, TableQuery[Results]) }
      .flatMap { case (row, table) =>
        session.db.run((table returning table.map(_.id)) += row)
      }
      .map { _.get }
      .map { (_, TableQuery[ResultParams]) }
      .flatMap { case (resultId, table) =>
        Future.sequence(
          result.params.map { case (name: String, value: String) =>
            session.db.run(table += (resultId, name, value))
          }
        )
      }
    Behaviors.same
  }

  private def findResults(userId: Int, searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]): Behavior[ResultsRepoMsg] = {
    Slick.source {
      taskId match {
        case Some(id) =>
          sql"SELECT * FROM result WHERE task_id = $id AND task_id IN (SELECT id FROM task WHERE search_id = $searchId AND search_id IN (SELECT id FROM search WHERE user_id = $userId))".as[ResultRow]
        case None =>
          sql"SELECT * FROM result WHERE task_id IN (SELECT id FROM task WHERE search_id = $searchId AND search_id IN (SELECT id FROM search WHERE user_id = $userId))".as[ResultRow]
      }
    }.map { row =>
      Slick.source {
        sql"SELECT * FROM result_param WHERE result_id = ${row.id.get}".as[ResultParamRow]
      } .runWith(Sink.seq)
        .map { _.map(p => p.name -> p.value) }
        .map { _.sortWith(_._1 > _._2) }
        .map { ListMap.newBuilder.addAll(_).result }
        .map {
          Result(row.id, row.taskId, row.title, row.subtitle, row.price, row.currency, row.url, row.imgUrl, _)
        }
    }
      .runWith(Sink.seq)
      .flatMap { a => Future.sequence(a) }
      .map { ResultsAnswer }
      .andThen { replyTo ! _.get }

    Behaviors.same
  }
}