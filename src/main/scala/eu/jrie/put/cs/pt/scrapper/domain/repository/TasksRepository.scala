package eu.jrie.put.cs.pt.scrapper.domain.repository

import java.sql.Timestamp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import eu.jrie.put.cs.pt.scrapper.domain.repository.Repository.RepoMsg
import eu.jrie.put.cs.pt.scrapper.domain.repository.TasksRepository.{AddTask, EndTask, TaskResponse, TasksRepoMsg}
import eu.jrie.put.cs.pt.scrapper.model.Task
import eu.jrie.put.cs.pt.scrapper.model.db.Tables.TasksTable.Tasks

object TasksRepository {
  sealed trait TasksRepoMsg extends RepoMsg

  case class AddTask(task: Task, replyTo: ActorRef[TaskResponse]) extends TasksRepoMsg
  case class EndTask(id: String) extends TasksRepoMsg

  case class TaskResponse(id: String) extends TasksRepoMsg

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
}
