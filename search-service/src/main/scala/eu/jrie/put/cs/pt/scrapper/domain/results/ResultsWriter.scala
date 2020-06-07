package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.ActorSource
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.AddResults
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.WriteResult
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository.EndTask

import scala.concurrent.{ExecutionContextExecutor, Future}

object ResultsWriter {
  case class WriteResult(result: Result, last: Boolean)

  private val config = ConfigFactory.load().getConfig("service.results.writer")

  def apply(implicit session: SlickSession): Behavior[WriteResult] = Behaviors.setup(ctx => new ResultsWriter(ctx))
}

private class ResultsWriter(ctx: ActorContext[WriteResult])
                           (implicit session: SlickSession) extends AbstractBehavior[WriteResult](ctx) {

  import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.config

  import scala.concurrent.duration._

//  private val resultsRepo = context.spawn(Routers.pool(3)(ResultsRepository()), "ResultsRepoPool-ResultsWriter")
  private val resultsRepo = context.spawn(ResultsRepository(), "ResultsRepoPool-ResultsWriter")
  private val tasksRepo = context.spawn(TasksRepository(), "TasksRepo-ResultsWriter")

  private implicit val system: ActorSystem[Nothing] = ctx.system
  private implicit val executionContext: ExecutionContextExecutor = ctx.executionContext

  private val writer = ActorSource.actorRef[Result](
    completionMatcher = PartialFunction.empty,
    failureMatcher = PartialFunction.empty,
    bufferSize = config.getInt("bufferSize"),
    overflowStrategy = OverflowStrategy.fail
  ).groupedWithin(config.getInt("chunkSize"), config.getInt("chunkInterval").seconds)
//    .to(Sink.foreach { resultsRepo ! AddResults(_) })
    .to(Sink.foreachAsync(1) (_ => Future { resultsRepo ! AddResults(_) }))
    .run()

  override def onMessage(msg: WriteResult): Behavior[WriteResult] = {
    writer ! msg.result

    if (msg.last) {
      context.log.info("received last result for {}", msg.result.taskId)
      tasksRepo ! EndTask(msg.result.taskId)
    }

    Behaviors.same
  }
}
