package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, Routers}
import akka.stream.alpakka.slick.javadsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.AddResult
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.WriteResult
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository.EndTask

object ResultsWriter {
  case class WriteResult(result: Result, last: Boolean)

  def apply(implicit session: SlickSession): Behavior[WriteResult] = Behaviors.setup(ctx => new ResultsWriter(ctx))
}

private class ResultsWriter(ctx: ActorContext[WriteResult])
                           (implicit session: SlickSession) extends AbstractBehavior[WriteResult](ctx) {

  private val resultsRepo = context.spawn(Routers.pool(10)(ResultsRepository()), "ResultsRepoPool-ResultsWriter")
  private val tasksRepo = context.spawn(TasksRepository(), "TasksRepo-ResultsWriter")

  override def onMessage(msg: WriteResult): Behavior[WriteResult] = {
    resultsRepo ! AddResult(msg.result)

    if (msg.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      tasksRepo ! EndTask(msg.result.taskId)
    }

    Behaviors.same
  }
}