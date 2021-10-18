package com.gerritforge

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.kinesis.{
  CommittableRecord,
  KinesisSchedulerCheckpointSettings,
  KinesisSchedulerSourceSettings
}
import akka.stream.alpakka.kinesis.scaladsl.KinesisSchedulerSource
import akka.stream.scaladsl
import com.gerritforge.BridgeKinesisConsumer.schedulerSourceSettings
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  DefaultCredentialsProvider,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.{
  KinesisAsyncClient,
  KinesisAsyncClientBuilder
}
import software.amazon.kinesis.common.ConfigsBuilder
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.ShardRecordProcessorFactory

import java.net.URI
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class BridgeKinesisConsumer(
    bridgeConfig: BridgeConfig,
    actorSystem: ActorSystem[Nothing]
) {

  val logger = Logger(
    LoggerFactory.getLogger(BridgeKinesisConsumer.getClass.getName)
  )

  private val region =
    bridgeConfig.conf.getOrElse("kinesis.region", "us-east-1")
  private val endpoint = bridgeConfig.conf.get("kinesis.endpoint")

  implicit val amazonKinesisAsync: KinesisAsyncClient = {
    val awsCreds = AwsBasicCredentials.create(
      "",
      ""
    )
    val kinesisClientBuilder = KinesisAsyncClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
      .httpClient(AkkaHttpClient.builder.withActorSystem(actorSystem).build)
      .region(Region.of(region))

    endpoint.map(e => kinesisClientBuilder.endpointOverride(new URI(e)))
    kinesisClientBuilder.build
  }

  private val cloudWatchAsynClient = {
    val awsCreds = AwsBasicCredentials.create(
      "",
      ""
    )
    val clientBuilder = CloudWatchAsyncClient.builder
      .httpClient(AkkaHttpClient.builder.withActorSystem(actorSystem).build)
      .region(Region.of(region))
      .credentialsProvider(StaticCredentialsProvider.create(awsCreds))

    endpoint.map(e => clientBuilder.endpointOverride(new URI(e)))
    clientBuilder.build
  }

  private val dynamoDBAsynClient = {

    val awsCreds = AwsBasicCredentials.create(
      "",
      ""
    )
    val clientBuilder = DynamoDbAsyncClient
      .builder()
      .httpClient(AkkaHttpClient.builder.withActorSystem(actorSystem).build)
      .region(Region.of(region))
      .credentialsProvider(StaticCredentialsProvider.create(awsCreds))

    endpoint.map(e => clientBuilder.endpointOverride(new URI(e)))
    clientBuilder.build
  }

  lazy val bridgeKinesisConsumerSources
      : Map[String, scaladsl.Source[CommittableRecord, Future[Scheduler]]] = {
    bridgeConfig.topics.map { t =>
      logger.info(s"*** Creating Kinesis Source for topic: $t")
      t -> KinesisSchedulerSource(builderFor(t), schedulerSourceSettings)
        .log("kinesis-records", "Consumed record " + _.sequenceNumber)
    }.toMap
  }

  def builderFor(streamName: String): ShardRecordProcessorFactory => Scheduler =
    recordProcessorFactory => {

      val configsBuilder = new ConfigsBuilder(
        streamName,
        "BridgeKinesisConsumer",
        amazonKinesisAsync,
        dynamoDBAsynClient,
        cloudWatchAsynClient,
        s"${
          import scala.sys.process._
          "hostname".!!.trim()
        }:${UUID.randomUUID()}",
        recordProcessorFactory
      )

      new Scheduler(
        configsBuilder.checkpointConfig,
        configsBuilder.coordinatorConfig,
        configsBuilder.leaseManagementConfig,
        configsBuilder.lifecycleConfig,
        configsBuilder.metricsConfig,
        configsBuilder.processorConfig,
        configsBuilder.retrievalConfig
      )
    }

}

object BridgeKinesisConsumer {
  val schedulerSourceSettings = KinesisSchedulerSourceSettings(
    bufferSize = 1000,
    backpressureTimeout = 1.minute
  )

  val checkpointSettings = KinesisSchedulerCheckpointSettings(100, 30.seconds)

}
