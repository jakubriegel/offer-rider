package eu.jrie.put.cs.pt.scrapper.redis

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.redis.Message.RedisMessage


object Publisher {
  trait PublisherMsg
  case class Publish(channel: String, msg: RedisMessage) extends PublisherMsg
  case class EndPublish() extends PublisherMsg

  def apply(client: RedisClient): Behavior[PublisherMsg] = Behaviors.receive { (context, message) =>
    message match {
      case Publish(channel, msg) =>
        context.log.trace("publishing {} on {}", asJson(msg), channel)
        client.publish(channel, asJson(msg))
        Behaviors.same
      case EndPublish() =>
        Behaviors.stopped
    }
  }

  def asJson(message: RedisMessage): String = {
    val mapper = new ObjectMapper()
    mapper.registerModule(new DefaultScalaModule())

    mapper.writeValueAsString(message)
  }
}