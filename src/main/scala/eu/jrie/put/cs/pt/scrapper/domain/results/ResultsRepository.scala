package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsParamsTable.ResultParams
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.Results

import scala.concurrent.{ExecutionContextExecutor, Future}

object ResultsRepository {
  sealed trait ResultsRepoMsg
  case class AddResult(result: Result) extends ResultsRepoMsg

  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  def apply(): Behavior[ResultsRepoMsg] = Behaviors.receive { (ctx, msg) =>
    implicit val system: ActorSystem[_] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = ctx.executionContext
    msg match {
      case AddResult(result) =>
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
      case _ =>
        ctx.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }
}
