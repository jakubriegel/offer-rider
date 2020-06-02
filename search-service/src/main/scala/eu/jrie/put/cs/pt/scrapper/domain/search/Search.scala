package eu.jrie.put.cs.pt.scrapper.domain.search

case class Search (id: Option[Int], userId: Int, params: Map[String, String], active: Boolean)
