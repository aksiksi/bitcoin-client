import sbt._
import sbt.Keys._

object LiftProjectBuild extends Build {

  import BuildSettings._

  lazy val root = Project("bitcoin", file("."))
    .settings(appSettings: _*)
    .settings(libraryDependencies ++=
      Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.3.3",
        "net.databinder.dispatch" %% "dispatch-lift-json" % "0.11.0",
        "com.palletops" % "java-websocket" % "1.3.1-SNAPSHOT",
        "ch.qos.logback" % "logback-classic" % "1.0.13" % "compile",
        "org.scalatest" %% "scalatest" % "1.9.2" % "test"
      )
    )
}
