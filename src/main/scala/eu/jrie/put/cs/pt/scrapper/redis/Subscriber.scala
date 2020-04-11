package eu.jrie.put.cs.pt.scrapper.redis

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.redis.{M, PubSubMessage, RedisClient}
import eu.jrie.put.cs.pt.scrapper.redis.Message.ResultMessage
import eu.jrie.put.cs.pt.scrapper.search.ResultsWriter
import eu.jrie.put.cs.pt.scrapper.search.ResultsWriter.WriteResult


object Subscriber {
  case class Subscribe(channel: String)

  val SEARCH_RESULTS_CHANNEL = "pt-scraper-results"

  def apply(client: RedisClient): Behavior[Subscribe] = Behaviors.receive { (context, msg) =>
    context.log.info("subscribing {}", msg.channel)

    val resultsWriter = context.spawn(ResultsWriter(), "resultsWriter")

    client.subscribe(msg.channel) { m: PubSubMessage =>
      m match {
        case M(channel, rawMsg) =>
          val msg: ResultMessage = asMsg(rawMsg)
          context.log.trace ("received {} from {}", msg.name, channel)
          resultsWriter ! WriteResult(msg)
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
