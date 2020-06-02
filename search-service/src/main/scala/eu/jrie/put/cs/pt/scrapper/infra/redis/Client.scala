package eu.jrie.put.cs.pt.scrapper.infra.redis

import com.redis.RedisClient

object Client {
  def apply() = new RedisClient("redis", 6379)
}
