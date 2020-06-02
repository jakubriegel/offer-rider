package eu.jrie.put.cs.pt.scrapper.infra.db

import eu.jrie.put.cs.pt.scrapper.domain.results.Result
import eu.jrie.put.cs.pt.scrapper.domain.tasks.Task
import slick.jdbc.GetResult
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

object Tables {
  object SearchesTable {
    class Searches (tag: Tag) extends Table[(Option[Int], Int, Boolean)](tag, "search") {
      def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
      def userId = column[Int]("user_id")
      def active = column[Boolean]("active")

      def * = (id, userId, active)
    }
  }

  object SearchesParamsTable {}

  object TasksTable {
    implicit val getTaskRow: AnyRef with GetResult[Task] = GetResult { r =>
      Task(
        r.nextString,
        r.nextInt,
        r.nextTimestamp.toInstant,
        r.nextTimestampOption.map { _.toInstant }
      )
    }
  }

  object ResultsTable {

    implicit val getResult: AnyRef with GetResult[Result] = GetResult { r =>
      Result(
        r.nextLongOption,
        r.nextString,
        r.nextStringOption,
        r.nextString,
        r.nextStringOption,
        r.nextDouble,
        r.nextString,
        r.nextString,
        r.nextStringOption,
        r.nextBoolean
      )
    }

    class Results (tag: Tag) extends Table[(Option[Long], String, Option[String], String, Option[String], Double, String, String, Option[String], Boolean)](tag, "result") {
      def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
      def taskId = column[String]("task_id")
      def offerId = column[Option[String]]("offer_id")
      def title = column[String]("title")
      def subtitle = column[Option[String]]("subtitle")
      def price = column[Double]("price")
      def currency = column[String]("currency")
      def url = column[String]("url")
      def imgUrl = column[Option[String]]("imgUrl")
      def newcomer = column[Boolean]("newcomer")

      def * = (id, taskId, offerId, title, subtitle, price, currency, url, imgUrl, newcomer)
    }
  }

  object ResultsParamsTable {}
}
