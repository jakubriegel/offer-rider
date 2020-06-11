package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, Routers}
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{Materializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.AddResults
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.WriteResult
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository.EndTask

object ResultsWriter {
  case class WriteResult(result: Result, last: Boolean)

  private val config = ConfigFactory.load().getConfig("service.results.writer")

  def apply(mat: Materializer)(implicit session: SlickSession): Behavior[WriteResult] =
    Behaviors.setup(ctx => new ResultsWriter(mat, ctx))
}

private class ResultsWriter(mat: Materializer, ctx: ActorContext[WriteResult])
                           (implicit session: SlickSession) extends AbstractBehavior[WriteResult](ctx) {

  import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.config

  import scala.concurrent.duration._

  private val resultsRepo = context.spawn(Routers.pool(10)(ResultsRepository()), "ResultsRepoPool-ResultsWriter")
  private val tasksRepo = context.spawn(TasksRepository(), "TasksRepo-ResultsWriter")

  private val writer = ActorSource.actorRef[Result](
    completionMatcher = PartialFunction.empty,
    failureMatcher = PartialFunction.empty,
    bufferSize = config.getInt("bufferSize"),
    overflowStrategy = OverflowStrategy.fail
  ).groupedWithin(config.getInt("chunkSize"), config.getInt("chunkInterval").seconds)
    .to(Sink.foreach { resultsRepo ! AddResults(_) })
    .run()(mat)

  override def onMessage(msg: WriteResult): Behavior[WriteResult] = {
    writer ! msg.result

    if (msg.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      tasksRepo ! EndTask(msg.result.taskId)
    }

    Behaviors.same
  }
}
