package eu.jrie.put.cs.pt.scrapper.model.db

import java.sql.Timestamp

import slick.jdbc.GetResult
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

object Tables {
  object SearchesTable {
    case class SearchRow(
                          id: Int,
                          userId: Int,
                          brand: String,
                          model: Option[String],
                          minMileage: Option[Int],
                          maxMileage: Option[Int],
                          active: Boolean
                        )

    implicit val getSearchRow: AnyRef with GetResult[SearchRow] = GetResult(r => {
      SearchRow(r.nextInt, r.nextInt, r.nextString, Option(r.nextString), Option(r.nextInt), Option(r.nextInt), r.nextBoolean)
    })
  }

  object TasksTable {
    class Tasks (tag: Tag) extends Table[(String, Int, Timestamp, Option[Timestamp])](tag, "task") {
      def id = column[String]("id")
      def searchId = column[Int]("search_id")
      def startTime = column[Timestamp]("start_time")
      def endTime = column[Option[Timestamp]]("end_time")

      def * = (id, searchId, startTime, endTime)
    }
  }

  object ResultsTable {
    case class ResultRow (taskId: String, name: String, link: String)

    class Results (tag: Tag) extends Table[(String, String, String)](tag, "result") {
      def taskId = column[String]("task_id")
      def name = column[String]("name")
      def link = column[String]("link")

      def * = (taskId, name, link)
    }
  }
}
