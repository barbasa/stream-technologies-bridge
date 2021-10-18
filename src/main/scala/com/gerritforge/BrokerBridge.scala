package com.gerritforge

import akka.actor.typed.ActorSystem
import akka.Done
import akka.kafka.{CommitterSettings, Subscriptions}
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka._
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.scaladsl.{Sink, Source}
import com.contxt.kinesis.KinesisRecord
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

object BrokerBridge extends App {

  val logger = Logger(LoggerFactory.getLogger(BrokerBridge.getClass.getName))

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "BrokerBridge")
  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  private def forwardRecordsFromKafkaToKinesis(bridgeConfig: BridgeConfig) = {
    logger.info("\uD83D\uDE80 Forwarding messages from Kafka to Kinesis")
    val bridgeKinesisProducer = BridgeKinesisProducer(bridgeConfig)

    def writeToKinesis(topic: String, record: String): Future[Done] = {
      val data = ByteBuffer.wrap(record.getBytes(StandardCharsets.UTF_8))
      val kinesisProducer = bridgeKinesisProducer.kinesisProducers.get(topic)

      if (kinesisProducer.isEmpty) {
        new RuntimeException(s"Cannot find produce for topic: $topic")
        Future {
          Done.done()
        }
      } else {
        kinesisProducer.get.send("1", data).map(userRecordResult => {
          if(userRecordResult.isSuccessful) {
            Done.done()
          } else {
            new RuntimeException("Error while writing on Kinesis")
            Done.done()
          }
        })
      }

    }

    val control =
      Consumer
        .committableSource(BridgeKafkaConsumer(bridgeConfig).settings, Subscriptions.topics(bridgeConfig.topics))
        .mapAsync(1) { msg =>
          val topic = msg.record.topic
          val value = msg.record.value
          if (bridgeConfig.skipLocalMessages && value.contains(s"\"sourceInstanceId\":\"${bridgeConfig.instanceId}\"")) {
            logger.debug(s"Skipping message forwarding for topic '$topic'")

            Future {
              msg.committableOffset
            }

          } else {
            writeToKinesis(topic, value)
              .map(_ => msg.committableOffset)
          }
        }
        .toMat(Committer.sink(CommitterSettings(actorSystem)))(DrainingControl.apply)
        .run()

    sys.ShutdownHookThread {
      logger.info("\uD83D\uDD0C Shutting down bridge \uD83D\uDD0C")
      Await.result(control.drainAndShutdown, 1.minute)
      bridgeKinesisProducer.kinesisProducers.foreach{ case (topic, consumer) =>
        logger.info(s"\uD83D\uDD0C * Shutting Kinesis producer for topic $topic \uD83D\uDD0C")
        Await.result(consumer.shutdown(), 5.minutes)
      }
    }

  }

    private def forwardRecordsFromKinesisToKafka(bridgeConfig: BridgeConfig) = {
      logger.info("\uD83D\uDE80 Forwarding messages from Kinesis to Kafka")

      val bridgeKafkaProducer = BridgeKafkaProducer(bridgeConfig, actorSystem)
      val bridgeKinesisConsumer = BridgeKinesisConsumer(bridgeConfig, actorSystem)

      bridgeKinesisConsumer.bridgeKinesisConsumerSources.foreach{ case (topic: String, source: Source[KinesisRecord, Future[Done]]) =>
        source.filter { kinesisRecord =>
          if (bridgeConfig.onlyForwardLocalMessages) {
            if (kinesisRecord.data.utf8String.contains(s"\"sourceInstanceId\":\"${bridgeConfig.instanceId}\"")) {
              true
            }
            else {
              logger.info(s"Skipping message forwarding for topic '$topic'")
              kinesisRecord.markProcessed()
              false
            }
          } else {
            true
          }
        }.
        map{ kinesisRecord =>
                  logger.debug(s"*** Processing record of topic $topic")
                  val value = kinesisRecord.data.utf8String
                  kinesisRecord.markProcessed()
                  new ProducerRecord[String, String](topic, value)
                }
                .runWith(bridgeKafkaProducer.kafkaSink)
      }

      bridgeKinesisConsumer.handleShutDown()
    }

  if (args.length == 0) {
    logger.error("Missing parameter. You need to specify the config file directory and the bridge type")
  }

  //TODO make sure the following args are passed
  val configFile = args(0)
  val bridgeType = args(1)

  logger.info("\uD83C\uDF09 Starting bridge \uD83C\uDF09")
  args(1) match {
    case b if b.equalsIgnoreCase("kafkaToKinesis") => forwardRecordsFromKafkaToKinesis(BridgeConfig(configFile))
    case b if b.equalsIgnoreCase("kinesisToKafka") => forwardRecordsFromKinesisToKafka(BridgeConfig(configFile))
    case _  => logger.error("Invalid bridge type")
  }

}
