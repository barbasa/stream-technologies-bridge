package com.gerritforge

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl
import com.contxt.kinesis.{ConsumerConfig, KinesisRecord, KinesisSource}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.{
  InitialPositionInStream,
  InitialPositionInStreamExtended
}

import java.net.URI
import java.util.UUID
import scala.concurrent.Future

case class BridgeKinesisConsumer(
    bridgeConfig: BridgeConfig,
    actorSystem: ActorSystem[Nothing]
) {

  val logger: Logger = Logger(
    LoggerFactory.getLogger(BridgeKinesisConsumer.getClass.getName)
  )

  private val region =
    bridgeConfig.conf.getOrElse("kinesis.region", "us-east-1")
  private val endpoint = bridgeConfig.conf.get("kinesis.endpoint")

  lazy val bridgeKinesisConsumerSources
      : Map[String, scaladsl.Source[KinesisRecord, Future[Done]]] = {
    bridgeConfig.topics.map { t =>
      logger.info(s"*** Creating Kinesis Source for topic: $t")
      t -> {
        val consumerConfig = ConsumerConfig(
          streamName = t,
          appName = s"atLeastOnceApp-$t",
          workerId = generateWorkerId(),
          kinesisClient = amazonKinesisAsync,
          dynamoClient = dynamoDBAsynClient,
          cloudwatchClient = cloudWatchAsynClient,
          initialPositionInStreamExtended =
            InitialPositionInStreamExtended.newInitialPosition(
              InitialPositionInStream.LATEST
            ),
          coordinatorConfig = None,
          leaseManagementConfig = None,
          metricsConfig = None,
          retrievalConfig = None
        )

        KinesisSource(consumerConfig)
      }
    }.toMap
  }

  private def generateWorkerId() = {
    s"${
      import scala.sys.process._
      "hostname".!!.trim()
    }:${UUID.randomUUID()}"
  }

  private val httpClient = NettyNioAsyncHttpClient.builder
    .maxConcurrency(100)
    .maxPendingConnectionAcquires(10_000)
    .build

  private val dynamoDBAsynClient = {

    val clientBuilder = DynamoDbAsyncClient
      .builder()
      .httpClient(httpClient)
      .region(Region.of(region))

    endpoint.map(e => clientBuilder.endpointOverride(new URI(e)))
    clientBuilder.build
  }

  private val cloudWatchAsynClient = {

    val clientBuilder = CloudWatchAsyncClient.builder
      .httpClient(httpClient)
      .region(Region.of(region))

    endpoint.map(e => clientBuilder.endpointOverride(new URI(e)))
    clientBuilder.build
  }

  private val amazonKinesisAsync: KinesisAsyncClient = {
    val kinesisClientBuilder = KinesisAsyncClient
      .builder()
      .httpClient(httpClient)
      .region(Region.of(region))

    endpoint.map(e => kinesisClientBuilder.endpointOverride(new URI(e)))
    kinesisClientBuilder.build
  }

  def handleShutDown() = {
    sys.ShutdownHookThread {
      amazonKinesisAsync.close()
      cloudWatchAsynClient.close()
      dynamoDBAsynClient.close()
    }
  }

}
