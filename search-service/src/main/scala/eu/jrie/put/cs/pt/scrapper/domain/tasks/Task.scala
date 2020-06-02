package eu.jrie.put.cs.pt.scrapper.domain.tasks

import java.time.Instant

case class Task(id: String, searchId: Int, startTime: Instant, endTime: Option[Instant])
