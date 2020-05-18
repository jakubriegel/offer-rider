package eu.jrie.put.cs.pt.scrapper.domain.repository

import java.sql.Timestamp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Sink
import eu.jrie.put.cs.pt.scrapper.domain.repository.Repository.RepoMsg
import eu.jrie.put.cs.pt.scrapper.domain.repository.TasksRepository._
import eu.jrie.put.cs.pt.scrapper.model.Task
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.TasksTable.Tasks

object TasksRepository {
  sealed trait TasksRepoMsg extends RepoMsg

  case class AddTask(task: Task, replyTo: ActorRef[TaskResponse]) extends TasksRepoMsg
  case class EndTask(id: String) extends TasksRepoMsg
  case class FindTasks(userId: Int, searchId: Int, replyTo: ActorRef[TasksResponse]) extends TasksRepoMsg
  case class CheckForNotEndedTasks(replyTo: ActorRef[HasEmptyTasksResponse]) extends TasksRepoMsg
  case class EndTasksRepo() extends TasksRepoMsg

  case class TaskResponse(id: String) extends TasksRepoMsg
  case class TasksResponse(tasks: Seq[Task]) extends TasksRepoMsg
  case class HasEmptyTasksResponse(has: Boolean) extends TasksRepoMsg

  def apply()(implicit session: SlickSession): Behavior[TasksRepoMsg] =
    Behaviors.setup(implicit context => new TasksRepository)
}

private class TasksRepository(
                               implicit context: ActorContext[TasksRepoMsg],
                               protected implicit val session: SlickSession
                             ) extends Repository[TasksRepoMsg] {

  import session.profile.api._

  override def onMessage(msg: TasksRepoMsg): Behavior[TasksRepoMsg] = {
    msg match {
      case AddTask(result, replyTo) => addNewTask(result, replyTo)
      case EndTask(id) => endTask(id)
      case FindTasks(userId, searchId, replyTo) => findTasks(userId, searchId, replyTo)
      case CheckForNotEndedTasks(replyTo) => checkForNotEndedTasks(replyTo)
      case EndTasksRepo() =>
        Behaviors.stopped
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def addNewTask(task: Task, replyTo: ActorRef[TaskResponse]): Behavior[TasksRepoMsg] = {
    session.db.run {
      TableQuery[Tasks] += (task.id, task.searchId, Timestamp.from(task.startTime), task.endTime.map(Timestamp.from))
    } .map(_ => TaskResponse(task.id))
      .andThen { replyTo ! _.get }
    Behaviors.same
  }

  private def endTask(id: String): Behavior[TasksRepoMsg] = {
    session.db.run(sqlu"UPDATE task SET end_time = current_timestamp WHERE id = $id")
    Behaviors.same
  }

  private def findTasks(userId: Int, searchId: Int, replyTo: ActorRef[TasksRepository.TasksResponse]): Behavior[TasksRepoMsg] = {
    Slick.source {
      import eu.jrie.put.cs.pt.scrapper.model.db.Tables.TasksTable.getTaskRow
      sql"""SELECT * FROM task
            WHERE search_id = $searchId
            AND search_id IN (SELECT id FROM search WHERE user_id = $userId)""".as[Task]
    } .runWith(Sink.seq)
      .map { TasksResponse }
      .andThen { replyTo ! _.get }
    Behaviors.same
  }

  private def checkForNotEndedTasks(replyTo: ActorRef[HasEmptyTasksResponse]): Behavior[TasksRepoMsg] = {
    Slick.source {
      sql"""SELECT count(*) FROM task WHERE end_time IS NULL""".as[Int]
    } .runWith(Sink.headOption)
      .map { _.get }
      .map { _ > 0 }
      .map { HasEmptyTasksResponse }
      .andThen { replyTo ! _.get }

    Behaviors.same
  }
}
