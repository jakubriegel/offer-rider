package eu.jrie.put.cs.pt.scrapper

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import com.redis.RedisClient
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber.Subscribe
import eu.jrie.put.cs.pt.scrapper.search.SearchExecutor
import eu.jrie.put.cs.pt.scrapper.search.SearchExecutor.StartSearch

import scala.concurrent.duration.Duration


object SearchService extends App {

  val redisClient = new RedisClient("jrie.eu", 6379)
  implicit val searchExecutor: ActorSystem[StartSearch] = ActorSystem(SearchExecutor(redisClient), "searchExecutor")

  val delay = Duration(5, TimeUnit.SECONDS)
  val rate = Duration(3, TimeUnit.MINUTES)
  searchExecutor.scheduler.scheduleAtFixedRate(delay, rate) (() => {
    searchExecutor ! StartSearch()
  }) (searchExecutor.executionContext)

  val subscriber: ActorSystem[Subscribe] = ActorSystem(Subscriber(new RedisClient("jrie.eu", 6379)), "redisSubscriber")
  subscriber ! Subscribe("pt-scraper-results")
}

//val tasks = TableQuery[Tasks]
//            (tasks returning tasks.map(_.id)) += (uuid, searchId, timestamp, None)