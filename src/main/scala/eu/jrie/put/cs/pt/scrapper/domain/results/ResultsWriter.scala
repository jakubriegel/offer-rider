package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.domain.repository.ResultsRepository.{AddResult, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.model.Result

object ResultsWriter {
  case class WriteResult(result: Result, last: Boolean)

  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")
  import session.profile.api._

  def apply(resultsRepo: ActorRef[ResultsRepoMsg]): Behavior[WriteResult] = Behaviors.receive { (context, msg) =>
    resultsRepo ! AddResult(msg.result)

    if (msg.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      session.db.run(sqlu"UPDATE task SET end_time = current_timestamp WHERE id = ${msg.result.taskId};")
    }

    Behaviors.same
  }
}
