// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
