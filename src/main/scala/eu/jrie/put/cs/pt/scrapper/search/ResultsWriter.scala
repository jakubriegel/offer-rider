package eu.jrie.put.cs.pt.scrapper.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.ResultsTable.Results
import eu.jrie.put.cs.pt.scrapper.redis.Message.ResultMessage

object ResultsWriter {
  case class WriteResult(result: ResultMessage)

  implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  def apply(): Behavior[WriteResult] = Behaviors.receive { (context, msg) =>
    session.db.run(TableQuery[Results] += (msg.result.taskId, msg.result.name, msg.result.link))

    if (msg.result.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      session.db.run(sqlu"UPDATE task SET end_time = current_timestamp WHERE id = ${msg.result.taskId};")
    }

    Behaviors.same
  }
}
