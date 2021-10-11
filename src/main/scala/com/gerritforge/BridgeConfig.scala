package com.gerritforge

import com.typesafe.config.ConfigFactory

import scala.jdk.CollectionConverters._

case class BridgeConfig() {
  lazy val conf = ConfigFactory.load

  //TODO: Check if at least a topic is defined
  val topics: Set[String] =
    conf.getStringList("bridge.common.topics").asScala.toSet
  val onlyForwardLocalMessages: Boolean =
    conf.getBoolean("bridge.common.onlyForwardLocalMessages")
  val instanceId: String = conf.getString("bridge.common.instanceId")
}
