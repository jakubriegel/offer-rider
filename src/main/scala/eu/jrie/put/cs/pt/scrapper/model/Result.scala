package eu.jrie.put.cs.pt.scrapper.model

case class Result (
                    id: Option[Long],
                    taskId: String,
                    title: String,
                    subtitle: Option[String],
                    url: Option[String],
                    imgUrl: Option[String],
                    params: Map[String, String]
                  )
