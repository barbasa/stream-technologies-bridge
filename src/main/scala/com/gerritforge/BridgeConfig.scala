package com.gerritforge

import com.gerritforge.BridgeConfig.getConfig

import java.io.{File, FileInputStream}
import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.collection.mutable.{Map => MMap}

case class BridgeConfig(configFilePath: String) {
  lazy val conf: MMap[String,String] = getConfig(configFilePath)

  //TODO: Check if at least a topic is defined
  val topics: Set[String] = conf.getOrElse("common.topics", "").split(",").toSet
  val skipLocalMessages: Boolean = conf.getOrElse("common.skipLocalMessages", "false").toBoolean
  val instanceId: String = conf.getOrElse("common.instanceId", "")
}

object BridgeConfig {
  def getConfig(filePath: String): MMap[String,String] = {
    val properties: Properties = new Properties
    properties.load(new FileInputStream(new File(filePath + "/bridge.properties")))
    properties.asScala
  }
}
