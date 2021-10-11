package com.gerritforge

import com.gerritforge.BrokerBridge.actorSystem
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import akka.kafka._
import akka.actor.typed.scaladsl.adapter._

import scala.concurrent.duration.DurationInt

case class BridgeKafkaConsumer(bridgeConfig: BridgeConfig) {

  private val groupId =
    bridgeConfig.conf.getString("bridge.kafka.groupId")
  private val kafkaBootstrapServers = bridgeConfig.conf
    .getString("bridge.kafka.bootstrapServers")

  val settings = ConsumerSettings(
    actorSystem.toClassic,
    new StringDeserializer,
    new StringDeserializer
  ).withBootstrapServers(kafkaBootstrapServers)
    .withGroupId(groupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    .withStopTimeout(0.seconds)

}
