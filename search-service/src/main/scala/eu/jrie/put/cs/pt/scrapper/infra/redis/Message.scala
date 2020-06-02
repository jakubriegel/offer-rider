package eu.jrie.put.cs.pt.scrapper.infra.redis

import eu.jrie.put.cs.pt.scrapper.domain.results.Result

object Message {
  trait RedisMessage

  case class TaskMessage(taskId: String, params: Map[String, String]) extends RedisMessage
  case class ResultMessage(result: Result, last: Boolean) extends RedisMessage
}
