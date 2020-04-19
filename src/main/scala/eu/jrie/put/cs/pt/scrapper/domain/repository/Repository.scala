package eu.jrie.put.cs.pt.scrapper.domain.repository

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import eu.jrie.put.cs.pt.scrapper.domain.repository.Repository.RepoMsg

import scala.concurrent.ExecutionContextExecutor

object Repository {
  trait RepoMsg
}

abstract class Repository[M <: RepoMsg](implicit context: ActorContext[M]) extends AbstractBehavior[M](context) {
  protected implicit val system: ActorSystem[_] = context.system
  protected implicit val executionContext: ExecutionContextExecutor = context.executionContext
}
