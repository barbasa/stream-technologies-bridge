package com.googlesource.gerrit.plugins.multisite.tools

import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration}

object BridgeKinesisProducer {

  val conf: KinesisProducerConfiguration = new KinesisProducerConfiguration()
                .setAggregationEnabled(false)
                .setMaxConnections(1)
    .setRegion("us-east-1")
  .setKinesisEndpoint("http://localhost").setKinesisPort(4566)

  lazy val kinesisProducer = new KinesisProducer(conf)
}
