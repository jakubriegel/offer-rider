package eu.jrie.put.cs.pt.scrapper.model

import java.time.Instant

case class Task(id: String, searchId: Int, startTime: Instant, endTime: Option[Instant])
