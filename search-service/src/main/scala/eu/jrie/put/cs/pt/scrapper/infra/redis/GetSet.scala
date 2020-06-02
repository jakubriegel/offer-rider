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

  def apply(client: RedisClient): Behavior[GetSetMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Get(key, replyTo) =>
        client.get(key) match {
          case Some(value) => replyTo ! GetResponse(Option(value))
          case _ => replyTo ! GetResponse(Option.empty)
        }
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
