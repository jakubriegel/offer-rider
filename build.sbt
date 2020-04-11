name := "scrapper-search-service"

version := "0.1"
scalaVersion := "2.13.1"
mainClass in assembly := Some("eu.jrie.put.cs.pt.scrapper.SearchService")

// akka
libraryDependencies += "com.typesafe.akka" %% "akka-typed" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.4"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "2.0.0-RC1"
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.11"

// mysql
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.2"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.19"

// redis
libraryDependencies += "net.debasishg" %% "redisclient" % "3.20"

// jackson
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.3"

// util
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}
