package com.gerritforge

import akka.actor.typed.ActorSystem
import akka.Done
import akka.kafka.{CommitterSettings, Subscriptions}
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka._
import akka.kafka.scaladsl.Consumer.DrainingControl
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

object BrokerBridge extends App {

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "BrokerBridge")
  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  //TODO add logic to skip local produced messages

  private def forwardRecordsFromKafkaToKinesis(bridgeConfig: BridgeConfig) = {
    println("\uD83D\uDE80 Forwarding messages from Kafka to Kinesis")

    implicit def toScalaFuture[T](lFuture: ListenableFuture[T]): Future[T] = {
      val p = Promise[T]
      val x = new FutureCallback[T] {

        def onSuccess(result: T): Unit = p.success(result)
        def onFailure(t: Throwable): Unit = p.failure(t)

      }
      Futures.addCallback(lFuture,x, Executors.newFixedThreadPool(1))
      p.future
    }

    def writeToKinesis(topic: String, record: String): Future[Done] = {
      val data = ByteBuffer.wrap(record.getBytes(StandardCharsets.UTF_8))
      val userRecordResultF: ListenableFuture[UserRecordResult] = BridgeKinesisProducer(bridgeConfig).kinesisProducer.addUserRecord(topic, "1", data)
      userRecordResultF.map(userRecordResult => {
        if(userRecordResult.isSuccessful) {
          Done.done()
        } else {
          new RuntimeException("Error while writing on Kinesis")
          Done.done()
        }
      })

    }

    val control =
      Consumer
        .committableSource(BridgeKafkaConsumer(bridgeConfig).settings, Subscriptions.topics(bridgeConfig.topics))
        .mapAsync(1) { msg =>
          val topic = msg.record.topic
          val value = msg.record.value
          if (bridgeConfig.skipLocalMessages && value.contains(s"\"sourceInstanceId\":\"${bridgeConfig.instanceId}\"")) {
            println(s"Skipping message forwarding for topic '$topic'")

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
      println("\uD83D\uDD0C Shutting down bridge \uD83D\uDD0C")
      Await.result(control.drainAndShutdown, 1.minute)
    }

  }

  private def forwardRecordsFromKinesisToKafka() {
  }


  if (args.length == 0) {
    println("Missing parameter. You need to specify the config file directory and the bridge type")
  }

  //TODO make sure the following args are passed
  val configFile = args(0)
  val bridgeType = args(1)

  println("\uD83C\uDF09 Starting bridge \uD83C\uDF09")
  args(1) match {
    case b if b.equalsIgnoreCase("kafkaToKinesis") => forwardRecordsFromKafkaToKinesis(BridgeConfig(configFile))
    case _  => println("Invalid bridge type")
  }

}
