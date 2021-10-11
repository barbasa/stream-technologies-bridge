package com.gerritforge

import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.contxt.kinesis.ScalaKinesisProducer
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

case class BridgeKinesisProducer(bridgeConfig: BridgeConfig) {

  val logger = Logger(
    LoggerFactory.getLogger(BridgeKinesisProducer.getClass.getName)
  )

  private val region =
    bridgeConfig.conf.getString("bridge.kinesis.region")
  private val endpoint =
    bridgeConfig.conf.getString("bridge.kinesis.endpoint") match {
      case "" => None
      case x  => Some(x)
    }

  val conf: KinesisProducerConfiguration = {
    val producerConfiguration = new KinesisProducerConfiguration()
      .setAggregationEnabled(false)
      .setMaxConnections(1)
      .setRegion(region)
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
