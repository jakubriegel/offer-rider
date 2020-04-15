package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsParamsTable.ResultParams
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.Results

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}

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
        val resultRow = (None, result.taskId, result.title, result.subtitle, result.url, result.imgUrl) //ResultRow(None, result.taskId, result.title, result.subtitle, result.url, result.imgUrl)
        val results = TableQuery[Results]
        val resultId = Await.result(
          session.db.run((results returning results.map(_.id)) += resultRow ),
          Duration.Inf
        ).get

        val resultParams = TableQuery[ResultParams]
        result.params foreach { case (name: String, value: String) =>
          session.db.run(resultParams += (resultId, name, value))
        }
        Behaviors.same
      case _ =>
        ctx.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }
}
