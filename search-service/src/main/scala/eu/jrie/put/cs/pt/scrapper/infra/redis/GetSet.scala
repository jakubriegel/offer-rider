package eu.jrie.put.cs.pt.scrapper.infra.redis

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object GetSet {
  sealed trait GetSetMsg
  case class Get(key: String, replyTo: ActorRef[GetResponse]) extends GetSetMsg
  case class GetResponse(value: Option[String]) extends GetSetMsg
  case class SetKey(key: String, value: String) extends GetSetMsg
  case class EndGetSet() extends GetSetMsg

  private val client = Client()

  def apply(): Behavior[GetSetMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Get(key, replyTo) =>
        replyTo ! GetResponse(
          if (client.exists(key)) client.get(key)
          else Option.empty
        )
        Behaviors.same
      case SetKey(key, value) =>
        client.set(key, value)
        client.expire(key, 600)
        Behaviors.same
      case EndGetSet() =>
        client.close()
        Behaviors.stopped
      case _ =>
        ctx.log.error("Invalid message")
        Behaviors.stopped
    }
  }
}
