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

import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.contxt.kinesis.ScalaKinesisProducer
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

case class BridgeKinesisProducer(bridgeConfig: BridgeConfig) {

  val logger = Logger(
    LoggerFactory.getLogger(BridgeKinesisProducer.getClass.getName)
  )

  private val endpoint =
    bridgeConfig.conf.getString("bridge.kinesis.endpoint") match {
      case "" => None
      case x  => Some(x)
    }

  val conf: KinesisProducerConfiguration = {
    val producerConfiguration = new KinesisProducerConfiguration()
      .setAggregationEnabled(false)
      .setMaxConnections(1)
      .setVerifyCertificate(false)

    endpoint.map { e =>
      val segments = e.split(":")
      //TODO: Check if url has been correctly set
      producerConfiguration
        .setKinesisEndpoint(segments(0))
        .setKinesisPort(segments(1).toLong)
    }
    producerConfiguration
  }

  lazy val kinesisProducers: Map[String, ScalaKinesisProducer] = {
    bridgeConfig.topics.map { t =>
      logger.info(s"*** Creating Kinesis producer for topic: $t")
      t -> ScalaKinesisProducer.apply(t, conf)
    }.toMap
  }
}
