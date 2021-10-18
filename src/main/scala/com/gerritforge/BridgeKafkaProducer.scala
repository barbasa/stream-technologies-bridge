package com.gerritforge

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.common.serialization.StringSerializer

case class BridgeKafkaProducer(
    bridgeConfig: BridgeConfig,
    actorSystem: ActorSystem[Nothing]
) {

  private val kafkaBootstrapServers = bridgeConfig.conf
    .getOrElse("kafka.bootstrapServers", "http://localhost:9092")

  val producerSettings = {
    ProducerSettings(
      actorSystem.toClassic,
      new StringSerializer,
      new StringSerializer
    ).withBootstrapServers(kafkaBootstrapServers)
  }

  val kafkaSink = Producer.plainSink(producerSettings)

}
