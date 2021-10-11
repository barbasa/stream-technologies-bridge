package com.googlesource.gerrit.plugins.multisite.tools

import akka.actor.typed.ActorSystem
import akka.{Done, NotUsed}
import akka.kafka.{CommitterSettings, KafkaConsumerActor, Subscriptions}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka._
import akka.kafka.scaladsl.Consumer.DrainingControl
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

  private def forwardRecordsFromKafkaToKinesis() = {


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
      val y = BridgeKinesisProducer.kinesisProducer
      val x = y.addUserRecord(topic, "1", data)
      x.map(x => {
        if(x.isSuccessful) {
          Done.done()
        } else {
          new RuntimeException("Error while writing on Kinesis")
          Done.done()
        }
      })

    }

    val control =
      Consumer
        .committableSource(BridgeKafkaConsumer.settings, Subscriptions.topics(BridgeConfig.topics))
        .mapAsync(1) { msg =>
          writeToKinesis(msg.record.topic, msg.record.value)
            .map(_ => msg.committableOffset)
        }
        .toMat(Committer.sink(CommitterSettings(actorSystem)))(DrainingControl.apply)
        .run()


    sys.ShutdownHookThread {
      println("\uD83D\uDD0C Shutting down bridge \uD83D\uDD0C")
      Await.result(control.drainAndShutdown, 1.minute)
    }

  }

  println("\uD83C\uDF09 Starting bridge \uD83C\uDF09")
  forwardRecordsFromKafkaToKinesis()

}
