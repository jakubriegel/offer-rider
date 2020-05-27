package eu.jrie.put.cs.pt.scrapper

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.util.Timeout
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import eu.jrie.put.cs.pt.scrapper.api.RestApi
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator
import eu.jrie.put.cs.pt.scrapper.domain.search.SearchTaskCreator.{CreateForAllActive, SearchTaskCreatorMsg}
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber
import eu.jrie.put.cs.pt.scrapper.redis.Subscriber.Subscribe

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Random

class SearchService {

  private def redisClient = new RedisClient("redis", 6379)
  private implicit val session: SlickSession = SlickSession.forConfig("slick-mysql")

  private val searchTaskCreator: ActorSystem[SearchTaskCreatorMsg] = ActorSystem(SearchTaskCreator(redisClient), "searchExecutor")
  private val subscriber: ActorSystem[Subscribe] = ActorSystem(Subscriber(redisClient), "subscriber")

  private val config = ConfigFactory.load().getConfig("service")

  private implicit val timeout: Timeout = 15.seconds

  def start(): Unit = {
    scheduleSearches()
    subscribeForEvents()
    initializeApi()
  }

  private def scheduleSearches(): Unit = {
    implicit val system: ActorSystem[SearchTaskCreatorMsg] = searchTaskCreator
    implicit val executionContext: ExecutionContextExecutor = searchTaskCreator.executionContext

    val delay = config.getInt("search.delay")
    val rate = config.getInt("search.interval")

    scheduleSearch(delay, rate)
    searchTaskCreator.log.info(s"scheduled tasks with delay ${delay}s and interval ${rate}s")
  }

  private def scheduleSearch(delay: Int, rate: Int)(implicit system: ActorSystem[SearchTaskCreatorMsg], executionContext: ExecutionContextExecutor): Unit = {
   searchTaskCreator.scheduler.scheduleOnce(delay.seconds, () => {
      (searchTaskCreator ? CreateForAllActive).onComplete { _ =>
        scheduleNextSearch(rate)
      }
    })
  }

  private def scheduleNextSearch(rate: Int): Unit = {
    implicit val system: ActorSystem[SearchTaskCreatorMsg] = searchTaskCreator
    implicit val executionContext: ExecutionContextExecutor = searchTaskCreator.executionContext

    val rateDiff = rate / 10
    val delay = rate - rateDiff + Random.nextInt(2*rateDiff + 1)

    scheduleSearch(delay, rate)
    searchTaskCreator.log.info(s"scheduled next tasks with delay ${delay}s")
  }

  private def subscribeForEvents(): Unit = {
    subscriber ! Subscribe(Subscriber.SEARCH_RESULTS_CHANNEL)
  }

  private def initializeApi(): Unit = {
    RestApi.run(searchTaskCreator)
  }
}

object SearchService extends App {
  val service = new SearchService()
  service.start()
}
