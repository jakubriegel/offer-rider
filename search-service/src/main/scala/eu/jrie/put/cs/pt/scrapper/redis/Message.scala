package eu.jrie.put.cs.pt.scrapper.redis

import eu.jrie.put.cs.pt.scrapper.model.Result

object Message {
  trait RedisMessage

  case class TaskMessage(taskId: String, params: Map[String, String]) extends RedisMessage
  case class ResultMessage(result: Result, last: Boolean) extends RedisMessage
}
