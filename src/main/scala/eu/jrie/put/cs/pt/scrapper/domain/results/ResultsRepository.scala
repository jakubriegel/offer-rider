package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Sink
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.{ResultsAnswer, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsParamsTable.ResultParams
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.{ResultRow, Results}

import scala.concurrent.{ExecutionContextExecutor, Future}

object ResultsRepository {
  sealed trait ResultsRepoMsg

  case class AddResult(result: Result) extends ResultsRepoMsg
  case class FindResults(searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]) extends ResultsRepoMsg

  case class ResultsAnswer(results: Seq[Result]) extends ResultsRepoMsg

  def apply(): Behavior[ResultsRepoMsg] =  Behaviors.setup[ResultsRepoMsg](implicit context => new ResultsRepository)
}

class ResultsRepository(implicit context: ActorContext[ResultsRepoMsg]) extends AbstractBehavior[ResultsRepoMsg](context) {
  import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.{AddResult, FindResults, ResultsRepoMsg}

  private implicit val system: ActorSystem[_] = context.system
  private implicit val executionContext: ExecutionContextExecutor = context.executionContext
  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  override def onMessage(msg: ResultsRepoMsg): Behavior[ResultsRepoMsg] = {
    msg match {
      case AddResult(result) => addNewResult(result)
      case FindResults(searchId, taskId, replyTo) => findResults(searchId, taskId, replyTo)
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def addNewResult(result: Result): Behavior[ResultsRepoMsg] = {
    Future {
      (None, result.taskId, result.title, result.subtitle, result.url, result.imgUrl)
    }.map { (_, TableQuery[Results]) }
      .flatMap { case (row, table) =>
        session.db.run((table returning table.map(_.id)) += row)
      }
      .map { _.get }
      .map { (_, TableQuery[ResultParams]) }
      .map { case (resultId, table) =>
        result.params foreach { case (name: String, value: String) =>
          session.db.run(table += (resultId, name, value))
        }
      }
    Behaviors.same
  }

  private def findResults(searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]): Behavior[ResultsRepoMsg] = {
    Slick.source {
      sql"SELECT * FROM result WHERE task_id IN (SELECT id FROM task WHERE search_id = 1)".as[ResultRow]
    }.map { r => Result(r.id, r.taskId, r.title, r.subtitle, r.url, r.imgUrl, Map.empty) }
        .runWith(Sink.seq)
        .map { ResultsAnswer }
        .andThen { replyTo ! _.get }
    Behaviors.same
  }
}