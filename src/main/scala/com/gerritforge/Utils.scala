package com.gerritforge

import com.google.common.util.concurrent.{
  FutureCallback,
  Futures,
  ListenableFuture
}

import java.util.concurrent.Executors
import scala.concurrent.{Future, Promise}

object Utils {

  implicit def toScalaFuture[T](lFuture: ListenableFuture[T]): Future[T] = {
    val p = Promise[T]
    val x = new FutureCallback[T] {

      def onSuccess(result: T): Unit = p.success(result)
      def onFailure(t: Throwable): Unit = p.failure(t)

    }
    Futures.addCallback(lFuture, x, Executors.newFixedThreadPool(1))
    p.future
  }

}
