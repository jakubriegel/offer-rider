package eu.jrie.put.cs.pt.scrapper.infra.redis

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.redis.{M, PubSubMessage, RedisClient}
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.WriteResult
import eu.jrie.put.cs.pt.scrapper.domain.results.{ResultsRepository, ResultsWriter}
import eu.jrie.put.cs.pt.scrapper.domain.tasks.TasksRepository
import eu.jrie.put.cs.pt.scrapper.infra.json.Mapper
import eu.jrie.put.cs.pt.scrapper.infra.redis.Message.ResultMessage

object Subscriber {
  case class Subscribe(channel: String)

  val SEARCH_RESULTS_CHANNEL = "pt-scraper-results"
  private val mapper = Mapper()

  def apply(client: RedisClient)(implicit session: SlickSession): Behavior[Subscribe] = Behaviors.receive { (context, msg) =>
    context.log.info("subscribing {}", msg.channel)

    val resultsRepo = context.spawn(ResultsRepository(), "resultsRepoResultsWriter")
    val tasksRepo = context.spawn(TasksRepository(), "tasksRepoResultsWriter")
    val resultsWriter = context.spawn(ResultsWriter(resultsRepo, tasksRepo), "resultsWriter")

    client.subscribe(msg.channel) { m: PubSubMessage =>
      m match {
        case M(channel, rawMsg) =>
          val msg: ResultMessage = asMsg(rawMsg)
          context.log.debug ("received {} for task {} from {}", msg.result.title, msg.result.taskId, channel)
          resultsWriter ! WriteResult(msg.result, msg.last)
        case _ =>
      }
    }

    Behaviors.same
  }

  private def asMsg(json: String): ResultMessage = {
    mapper.readValue(json, classOf[ResultMessage])
  }
}
