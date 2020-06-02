package eu.jrie.put.cs.pt.scrapper.infra.redis

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.redis.RedisClient

object GetSet {
  sealed trait GetSetMsg
  case class Get(key: String, replyTo: ActorRef[GetResponse]) extends GetSetMsg
  case class GetResponse(value: Option[String]) extends GetSetMsg
  case class SetKey(key: String, value: String) extends GetSetMsg
  case class EndGetSet() extends GetSetMsg

  def apply(client: RedisClient): Behavior[GetSetMsg] = Behaviors.receive { (context, msg) =>
    msg match {
      case Get(key, replyTo) =>
        replyTo ! GetResponse(client.getType(key))
        Behaviors.same
      case SetKey(key, value) =>
        client.set(key, value)
        client.expire(key, 600)
        Behaviors.same
      case EndGetSet() =>
        Behaviors.stopped
    }
  }
}
