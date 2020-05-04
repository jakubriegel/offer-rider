package eu.jrie.put.cs.pt.scrapper.redis

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.redis.{M, PubSubMessage, RedisClient}
import eu.jrie.put.cs.pt.scrapper.domain.repository.{ResultsRepository, TasksRepository}
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.WriteResult
import eu.jrie.put.cs.pt.scrapper.model.Result
import eu.jrie.put.cs.pt.scrapper.redis.Message.ResultMessage

object Subscriber {
  case class Subscribe(channel: String)

  val SEARCH_RESULTS_CHANNEL = "pt-scraper-results"

  def apply(client: RedisClient)(implicit session: SlickSession): Behavior[Subscribe] = Behaviors.receive { (context, msg) =>
    context.log.info("subscribing {}", msg.channel)

    val resultsRepo = context.spawn(ResultsRepository(), "resultsRepoResultsWriter")
    val tasksRepo = context.spawn(TasksRepository(), "tasksRepoResultsWriter")
    val resultsWriter = context.spawn(ResultsWriter(resultsRepo, tasksRepo), "resultsWriter")

    client.subscribe(msg.channel) { m: PubSubMessage =>
      m match {
        case M(channel, rawMsg) =>
          val msg: ResultMessage = asMsg(rawMsg)
          context.log.debug ("received {} for task {} from {}", msg.title, msg.taskId, channel)
          val result = Result(None, msg.taskId, msg.title, msg.subtitle, msg.price, msg.currency, msg.url, msg.imgUrl, msg.params)
          resultsWriter ! WriteResult(result, msg.last)
        case _ =>
      }
    }

    Behaviors.same
  }

  private def asMsg(json: String): ResultMessage = {
    val mapper = new ObjectMapper()
    mapper.registerModule(new DefaultScalaModule())
    mapper.readValue(json, classOf[ResultMessage])
  }
}
