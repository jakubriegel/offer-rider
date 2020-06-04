package eu.jrie.put.cs.pt.scrapper.infra.json

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Mapper {
  def apply(): ObjectMapper = new ObjectMapper()
    .registerModule(new DefaultScalaModule)
    .registerModule(new JavaTimeModule)
    .registerModule(new Jdk8Module)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
}
