package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import eu.jrie.put.cs.pt.scrapper.domain.repository.ResultsRepository.{AddResult, ResultsRepoMsg}
import eu.jrie.put.cs.pt.scrapper.domain.repository.TasksRepository.{EndTask, TasksRepoMsg}
import eu.jrie.put.cs.pt.scrapper.model.Result

object ResultsWriter {
  case class WriteResult(result: Result, last: Boolean)

  def apply(resultsRepo: ActorRef[ResultsRepoMsg], tasksRepo: ActorRef[TasksRepoMsg]): Behavior[WriteResult] = Behaviors.receive { (context, msg) =>
    resultsRepo ! AddResult(msg.result)

    if (msg.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      tasksRepo ! EndTask(msg.result.taskId)
    }

    Behaviors.same
  }
}
