package eu.jrie.put.cs.pt.scrapper.model

case class Search(
                   id: Int,
                   userId: Int,
                   params: SearchParams,
                   active: Boolean
                 )
