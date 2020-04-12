package eu.jrie.put.cs.pt.scrapper.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsParamsTable.ResultParams
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.Results
import eu.jrie.put.cs.pt.scrapper.redis.Message.ResultMessage

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ResultsWriter {
  case class WriteResult(result: ResultMessage)

  implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  def apply(): Behavior[WriteResult] = Behaviors.receive { (context, msg) =>
    val result = (None, msg.result.taskId, msg.result.title, msg.result.subtitle, msg.result.url, msg.result.imgUrl)
    val results = TableQuery[Results]
    val resultId = Await.result(
      session.db.run((results returning results.map(_.id)) += result),
      Duration.Inf
    ).get

    val resultParams = TableQuery[ResultParams]
    msg.result.params foreach { case (name: String, value: String) =>
      session.db.run(resultParams += (resultId, name, value))
    }

    if (msg.result.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      session.db.run(sqlu"UPDATE task SET end_time = current_timestamp WHERE id = ${msg.result.taskId};")
    }

    Behaviors.same
  }
}

//val tasks = TableQuery[Tasks]
//            (tasks returning tasks.map(_.id)) += (uuid, searchId, timestamp, None)