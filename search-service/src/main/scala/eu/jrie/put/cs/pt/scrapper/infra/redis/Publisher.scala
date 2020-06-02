package eu.jrie.put.cs.pt.scrapper.infra.redis

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.infra.json.Mapper
import eu.jrie.put.cs.pt.scrapper.infra.redis.Message.RedisMessage


object Publisher {

  trait PublisherMsg
  case class Publish(channel: String, msg: RedisMessage) extends PublisherMsg
  case class EndPublish() extends PublisherMsg

  private val mapper = Mapper()

  def apply(implicit client: RedisClient): Behavior[PublisherMsg] = Behaviors.receive { (ctx, message) =>
    message match {
      case Publish(channel, msg) =>
        ctx.log.debug("publishing {} on {}", msg, channel)
        client.publish(channel, msg)
        Behaviors.same
      case EndPublish() =>
        Behaviors.stopped
    }
  }

  private implicit def asJson(message: RedisMessage): String = {
    mapper.writeValueAsString(message)
  }
}
