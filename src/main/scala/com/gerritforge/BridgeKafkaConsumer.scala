package com.gerritforge

import com.gerritforge.BrokerBridge.actorSystem
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import akka.kafka._
import akka.actor.typed.scaladsl.adapter._

import scala.concurrent.duration.DurationInt

case class BridgeKafkaConsumer(bridgeConfig: BridgeConfig) {

  private val groupId =
    bridgeConfig.conf.getOrElse("kafka.groupId", "BrokerBridgeConsumer")
  private val kafkaBootstrapServers = bridgeConfig.conf
    .getOrElse("kafka.bootstrapServers", "http://localhost:9092")

  val settings = ConsumerSettings(
    actorSystem.toClassic,
    new StringDeserializer,
    new StringDeserializer
  ).withBootstrapServers(kafkaBootstrapServers)
    .withGroupId(groupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withStopTimeout(0.seconds)

}
