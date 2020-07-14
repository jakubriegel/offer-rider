package eu.jrie.put.cs.pt.scrapper.infra.redis

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.Materializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.redis.{M, PubSubMessage}
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsWriter.WriteResult
import eu.jrie.put.cs.pt.scrapper.infra.json.Mapper
import eu.jrie.put.cs.pt.scrapper.infra.redis.Message.ResultMessage

object Subscriber {
  case class Subscribe(channel: String)

  val SEARCH_RESULTS_CHANNEL = "pt-scraper-results"
  private val mapper = Mapper()
  private val client = Client()

  def apply()(implicit session: SlickSession): Behavior[Subscribe] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info("subscribing {}", msg.channel)

    val resultsWriter = ctx.spawn(ResultsWriter(Materializer(ctx)), "ResultsWriter-Subscriber")
    implicit val system: ActorSystem[Nothing] = ctx.system
    client.subscribe(msg.channel) { m: PubSubMessage =>
      m match {
        case M(channel, rawMsg) =>
          val msg: ResultMessage = asMsg(rawMsg)
          ctx.log.debug ("received {} for task {} from {}", msg.result.title, msg.result.taskId, channel)
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
