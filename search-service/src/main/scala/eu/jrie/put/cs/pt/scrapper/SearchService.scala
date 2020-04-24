package eu.jrie.put.cs.pt.scrapper

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.api.RestApi
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchExecutor
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchExecutor.StartSearch
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber.Subscribe

import scala.concurrent.duration.Duration

class SearchService {

  private def redisClient = new RedisClient("jrie.eu", 6379)
  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")

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
    subscriber ! Subscribe(Subscriber.SEARCH_RESULTS_CHANNEL)
  }

  private def initializeApi(): Unit = {
    RestApi.run
  }
}

object SearchService extends App {
  val service = new SearchService()
  service.start()
}
