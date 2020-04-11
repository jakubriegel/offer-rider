package eu.jrie.put.cs.pt.scrapper

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.api.RestApi
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber.Subscribe
import eu.jrie.put.cs.pt.scrapper.search.SearchExecutor
import eu.jrie.put.cs.pt.scrapper.search.SearchExecutor.StartSearch

import scala.concurrent.duration.Duration

class SearchService {

  private def redisClient = new RedisClient("jrie.eu", 6379)

  private val searchExecutor: ActorSystem[StartSearch] = ActorSystem(SearchExecutor(redisClient), "searchExecutor")
  private val subscriber: ActorSystem[Subscribe] = ActorSystem(Subscriber(redisClient), "subscriber")

  private val config = ConfigFactory.load().getConfig("service")

  def start(): Unit = {
    scheduleSearches()
    subscribeForEvents()
    initializeApi()
  }

  private def scheduleSearches(): Unit = {
    val delay = Duration(config.getInt("search.delay"), TimeUnit.SECONDS)
    val rate = Duration(config.getInt("search.interval"), TimeUnit.SECONDS)
    searchExecutor.scheduler.scheduleAtFixedRate(delay, rate) (() => {
      searchExecutor ! StartSearch()
    }) (searchExecutor.executionContext)
    searchExecutor.log.info(s"scheduled searches with delay ${delay}s and interval ${rate}s")
  }

  private def subscribeForEvents(): Unit = {
    subscriber ! Subscribe("pt-scraper-results")
  }

  private def initializeApi(): Unit = {
    RestApi.run
  }
}

object SearchService extends App {
  val service = new SearchService()
  service.start()
}

//val tasks = TableQuery[Tasks]
//            (tasks returning tasks.map(_.id)) += (uuid, searchId, timestamp, None)