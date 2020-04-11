package eu.jrie.put.cs.pt.scrapper.redis

import eu.jrie.put.cs.pt.scrapper.model.SearchParams

object Message {
  trait RedisMessage

  case class TaskMessage(taskId: String, params: SearchParams) extends RedisMessage
  case class ResultMessage(taskId: String, name: String, link: String, last: Boolean) extends RedisMessage
}
