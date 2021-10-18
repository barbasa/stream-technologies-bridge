import sbt._

object Dependencies {
  val scalaVer = "2.13.6"
  val AkkaVersion = "2.6.16"
  val AlpakkaVersion = "2.0.2"
  val AkkaHttpVersion = "10.1.11"
  val AlpakkaKafkaVersion = "2.0.5"
  val AlpakkaKinesisVersion = "3.0.3"

  val dependencies = List(
    "io.github.streetcontxt" %% "kcl-akka-stream" % "5.0.0",
    "com.amazonaws" % "aws-java-sdk-kinesis" % "1.12.82",
    "com.amazonaws" % "amazon-kinesis-producer" % "0.14.9",
    "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
    "io.github.streetcontxt" %% "kpl-scala" % "2.0.0"
  )
}
