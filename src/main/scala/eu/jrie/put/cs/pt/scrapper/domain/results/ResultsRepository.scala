package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.ResultsRepoMsg
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsParamsTable.ResultParams
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.Results

import scala.concurrent.{ExecutionContextExecutor, Future}

object ResultsRepository {
  sealed trait ResultsRepoMsg
  case class AddResult(result: Result) extends ResultsRepoMsg
  case class FindResults(searchId: String, taskId: Option[String]) extends ResultsRepoMsg

  def apply(): Behavior[ResultsRepoMsg] =  Behaviors.setup[ResultsRepoMsg](implicit context => new ResultsRepository)
}

class ResultsRepository(implicit context: ActorContext[ResultsRepoMsg]) extends AbstractBehavior[ResultsRepoMsg](context) {
  import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.{AddResult, FindResults, ResultsRepoMsg}

  private implicit val executionContext: ExecutionContextExecutor = context.executionContext
  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  override def onMessage(msg: ResultsRepoMsg): Behavior[ResultsRepoMsg] = {
    msg match {
      case AddResult(result) =>
        addNewResult(result)
        Behaviors.same
      case FindResults(searchId, taskId) =>

        Behaviors.same
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def addNewResult(result: Result): Unit = {
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
  }
}