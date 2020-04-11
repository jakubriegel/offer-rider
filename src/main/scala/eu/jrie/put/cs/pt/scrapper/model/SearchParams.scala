package eu.jrie.put.cs.pt.scrapper.model

case class SearchParams (
                          brand: String,
                          model: Option[String],
                          minMileage: Option[Int],
                          maxMileage: Option[Int]
                        )
