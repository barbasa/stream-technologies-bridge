import sbt._

object Dependencies {
  val scalaVer = "2.13.6"
  // #deps
  val AkkaVersion = "2.6.16"
  val AlpakkaVersion = "2.0.2"
  val AlpakkaKafkaVersion = "2.0.5"
  val AlpakkaKinesisVersion = "3.0.3"

  // #deps

  val dependencies = List(
    // #deps
    "com.lightbend.akka" %% "akka-stream-alpakka-kinesis" % AlpakkaKinesisVersion,
    "com.amazonaws" % "aws-java-sdk-kinesis" % "1.12.82",
    "com.amazonaws" % "amazon-kinesis-producer" % "0.14.9",
    "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    // for JSON in Scala
    "io.spray" %% "spray-json" % "1.3.5",
    // for JSON in Java
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.10.5",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.10.5",
    // Logging
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}
