import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName

organization := "com.gerritforge"
version := "0.0.2"
scalaVersion := Dependencies.scalaVer
libraryDependencies ++= Dependencies.dependencies
assembly / assemblyJarName := s"stream-technologies-bridge-${version.value}.jar"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf")    => MergeStrategy.concat
  case _                             => MergeStrategy.first
}
