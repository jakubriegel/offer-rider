package eu.jrie.put.cs.pt.scrapper.model.db

import java.sql.Timestamp

import slick.jdbc.GetResult
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

object Tables {
  object SearchesTable {
    case class SearchRow(id: Int, userId: Int, active: Boolean)

    implicit val getSearchRow: AnyRef with GetResult[SearchRow] = GetResult(r => {
      SearchRow(r.nextInt, r.nextInt, r.nextBoolean)
    })
  }

  object SearchesParamsTable {
    case class SearchParamRow(searchId: Int, name: String, value: String)

    implicit val getSearchRow: AnyRef with GetResult[SearchParamRow] = GetResult(r => {
      SearchParamRow(r.nextInt, r.nextString, r.nextString)
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
    case class ResultRow (
                           id: Long,
                           taskId: String,
                           title: String,
                           subtitle: Option[String],
                           url: Option[String],
                           imgUrl: Option[String]
                         )

    class Results (tag: Tag) extends Table[(Option[Long], String, String, Option[String], Option[String], Option[String])](tag, "result") {
      def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
      def taskId = column[String]("task_id")
      def title = column[String]("title")
      def subtitle = column[Option[String]]("subtitle")
      def url = column[Option[String]]("url")
      def imgUrl = column[Option[String]]("imgUrl")

      def * = (id, taskId, title, subtitle, url, imgUrl)
    }
  }

  object ResultsParamsTable {
    case class ResultParamRow(resultId: Long, name: String, value: String)

    implicit val getSearchRow: AnyRef with GetResult[ResultParamRow] = GetResult(r => {
      ResultParamRow(r.nextLong, r.nextString, r.nextString)
    })

    class ResultParams (tag: Tag) extends Table[(Long, String, String)](tag, "result_param") {
      def resultId = column[Long]("result_id")
      def name = column[String]("name")
      def value = column[String]("value")

      def * = (resultId, name, value)
    }
  }
}
