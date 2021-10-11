// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
