package eu.jrie.put.cs.pt.scrapper.model

case class Search (id: Option[Int], userId: Int, params: Map[String, String], active: Boolean)
