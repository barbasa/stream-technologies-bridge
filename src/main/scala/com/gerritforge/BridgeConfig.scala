package com.gerritforge

import com.gerritforge.BridgeConfig.getConfig

import java.io.{File, FileInputStream}
import java.util.Properties
import scala.jdk.CollectionConverters._

case class BridgeConfig(configFilePath: String) {
  lazy val conf: Map[String, String] = getConfig(configFilePath)

  //TODO: Check if at least a topic is defined
  val topics: Set[String] = conf.getOrElse("common.topics", "").split(",").toSet
  val onlyForwardLocalMessages: Boolean =
    conf.getOrElse("common.onlyForwardLocalMessages", "false").toBoolean
  val instanceId: String = conf.getOrElse("common.instanceId", "")
}

object BridgeConfig {
  def getConfig(filePath: String): Map[String, String] = {
    val properties: Properties = new Properties
    properties.load(
      new FileInputStream(new File(filePath + "/bridge.properties"))
    )
    properties.asScala.toMap
  }
}
