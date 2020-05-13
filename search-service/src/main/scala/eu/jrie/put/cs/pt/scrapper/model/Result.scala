package eu.jrie.put.cs.pt.scrapper.model

case class Result (
                    id: Option[Long],
                    taskId: String,
                    offerId: Option[String],
                    title: String,
                    subtitle: Option[String],
                    price: Double,
                    currency: String,
                    url: String,
                    imgUrl: Option[String],
                    params: Map[String, String]
                  )
