package eu.jrie.put.cs.pt.scrapper.domain.results

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.ResultsRepoMsg
import eu.jrie.put.cs.pt.scrapper.infra.Repository
import eu.jrie.put.cs.pt.scrapper.infra.Repository.RepoMsg
import eu.jrie.put.cs.pt.scrapper.infra.db.Tables.ResultsTable.{Results, getResult}
import eu.jrie.put.cs.pt.scrapper.infra.json.Mapper
import eu.jrie.put.cs.pt.scrapper.infra.redis.GetSet
import eu.jrie.put.cs.pt.scrapper.infra.redis.GetSet.{EndGetSet, Get, GetResponse, SetKey}

import scala.collection.immutable
import scala.collection.immutable.ListMap
import scala.concurrent.{Await, Future}

object ResultsRepository {
  sealed trait ResultsRepoMsg extends RepoMsg

  case class AddResults(results: Seq[Result]) extends ResultsRepoMsg
  case class FindResults(userId: Int, searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]) extends ResultsRepoMsg

  case class ResultsAnswer(results: Seq[Result]) extends ResultsRepoMsg

  def apply()(implicit session: SlickSession): Behavior[ResultsRepoMsg] =
    Behaviors.setup[ResultsRepoMsg](implicit context => new ResultsRepository)
}

private class ResultsRepository(
                                 implicit context: ActorContext[ResultsRepoMsg],
                                 protected implicit val session: SlickSession
                               ) extends Repository[ResultsRepoMsg] {
  import akka.actor.typed.scaladsl.AskPattern._
  import eu.jrie.put.cs.pt.scrapper.domain.results.ResultsRepository.{AddResults, FindResults, ResultsAnswer}
  import session.profile.api._

  import scala.concurrent.duration._
  private implicit val timeout: Timeout = 15.seconds

  private val getSet = context.spawn(GetSet(), s"ResultsRepositoryGetSet-$this")
  private val mapper = Mapper()

  override def onMessage(msg: ResultsRepoMsg): Behavior[ResultsRepoMsg] = {
    msg match {
      case AddResults(results) =>
        context.log.info(s"Adding ${results.length} results")
        addResults(results)
        Behaviors.same
      case FindResults(userId, searchId, taskId, replyTo) =>
        findResults(userId, searchId, taskId, replyTo)
        Behaviors.same
      case _ =>
        context.log.info("unsupported repo msg")
        getSet ! EndGetSet()
        Behaviors.stopped
    }
  }

  private def addResults(results: Seq[Result]): Unit = {
    results.groupBy { _.taskId }
      .foreachEntry { case (taskId, withId) =>
        val lastIds = findLastIds(taskId)
        val withFlag: Seq[Result] = Await.result(withNewcomerFlag(withId, lastIds), Duration.Inf)
        val added = Await.result(addResultsOfTask(withFlag), Duration.Inf)
        Await.ready(addParamsOfResults(added), Duration.Inf)
//        Await.ready(action, Duration.create(360, TimeUnit.MINUTES))
      }
  }

  private def findLastIds(taskId: String): Future[Seq[String]] = {
    val cachedIdsFuture: Future[GetResponse] = getSet ? (Get(s"lastIds-$taskId", _))
    cachedIdsFuture.map { _.value }
      .map { _.map { raw => mapper.readValue(raw, classOf[Seq[String]]) } }
      .flatMap { cached =>
        if (cached.isEmpty) {
          Slick.source {
            sql"""
                  SELECT offer_id FROM result
                  WHERE task_id = (
                    SELECT id FROM task
                    WHERE search_id = (SELECT search_id FROM task WHERE id = $taskId)
                    AND id != $taskId
                    ORDER BY end_time DESC LIMIT 1
                  );
                  """.as[String]
          }.runWith(Sink.seq)
            .andThen { ids => getSet ! SetKey(s"lastIds-$taskId", mapper.writeValueAsString(ids.get)) }
        } else {
          Future { cached.get }
        }
      }
  }

  private def withNewcomerFlag(results: Seq[Result], lastIds: Future[Seq[String]]): Future[Seq[Result]] = {
    val withNewcomer = results.map { Future(_) }
      .map { result =>
        result.zipWith(lastIds) { case (r, lastOfferIds) =>
          val newcomer = r.offerId
            .map { lastOfferIds.contains }
            .forall { !_ }
          Result(None, r.taskId, r.offerId, r.title, r.subtitle, r.price, r.currency, r.url, r.imgUrl, newcomer, r.params)
        }
      }
    Future.sequence(withNewcomer)
  }

  private def addResultsOfTask(results: Seq[Result]): Future[Seq[(Long, Map[String, String])]] = {
    val actionToParams = results.map { r =>
      ((None, r.taskId, r.offerId, r.title, r.subtitle, r.price, r.currency, r.url, r.imgUrl, r.newcomer), r.params)
    }
      .map { case (row, params) => (row, params, TableQuery[Results]) }
      .map { case (row, params, table) => ((table returning table.map(_.id)) += row, params) }

    val queries = actionToParams.map { _._1 }
    val params = actionToParams.map { _._2 }
    session.db.run(DBIO.sequence(queries))
      .map { ids =>
        ids.map { _.get }
          .zip(params)
      }
  }

  private def addParamsOfResults(idsToParams: Seq[(Long, Map[String, String])]): Future[Seq[immutable.Iterable[Int]]] = {
    context.log.info(s"Adding ${idsToParams.length} params")
    Future.sequence(
      idsToParams.map { case (resultId, params) => addParamsOfResult(resultId, params) }
    )
  }

  private def addParamsOfResult(resultId: Long, params: Map[String, String]): Future[immutable.Iterable[Int]] = {
    val query = params.filter { case (_, value) => value != null }
      .map { case (name, value) => sqlu"INSERT INTO result_param VALUES ($resultId, $name, $value)" }
    session.db.run(DBIO.sequence(query))
  }

  private def findResults(userId: Int, searchId: Long, taskId: Option[String], replyTo: ActorRef[ResultsAnswer]): Unit = {
    Slick.source {
      taskId match {
        case Some(id) =>
          sql"""
                SELECT * FROM result
                WHERE task_id = $id
                AND task_id IN (SELECT id FROM task WHERE search_id = $searchId AND search_id IN (SELECT id FROM search WHERE user_id = $userId))
                """.as[Result]
        case None =>
          sql"""
               SELECT * FROM result
               WHERE task_id IN (SELECT id FROM task WHERE search_id = $searchId AND search_id IN (SELECT id FROM search WHERE user_id = $userId))
               """.as[Result]
      }
    }.map { r =>
      Slick.source {
        sql"SELECT * FROM result_param WHERE result_id = ${r.id.get}".as[(Int, String, String)]
      } .map { case (_, name, value) => (name, value) }
        .runWith(Sink.seq)
        .map { _.sortWith(_._1 > _._2) }
        .map { ListMap.newBuilder.addAll(_).result }
        .map {
          Result(r.id, r.taskId, r.offerId, r.title, r.subtitle, r.price, r.currency, r.url, r.imgUrl, r.newcomer, _)
        }
    }
      .runWith(Sink.seq)
      .flatMap { a => Future.sequence(a) }
      .map { ResultsAnswer }
      .andThen { replyTo ! _.get }
  }
}