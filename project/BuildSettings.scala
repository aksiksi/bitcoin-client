import sbt._
import sbt.Keys._

import sbtbuildinfo.Plugin._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys
import ohnosequences.sbt.SbtS3Resolver._

object BuildSettings {
  val buildTime = SettingKey[String]("build-time")
  
  val defaultScalaVersion = "2.10.4"

  val basicSettings = Defaults.defaultSettings ++ Seq(
    name := "bitcoin",
    version := "0.1-SNAPSHOT",
    organization := "inc.pyc",
    scalaVersion := defaultScalaVersion,
    scalacOptions <<= scalaVersion map { sv: String =>
      if (sv.startsWith("2.10."))
        Seq("-deprecation", "-unchecked", "-feature", "-language:postfixOps", "-language:implicitConversions")
      else
        Seq("-deprecation", "-unchecked")
    },
    resolvers ++= Seq[Resolver](
        "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
        "clojars.org" at "http://clojars.org/repo"
    )
  )

  val appSettings = 
    basicSettings ++
    S3Resolver.defaults ++
    buildInfoSettings ++
    seq(
      buildTime := System.currentTimeMillis.toString,

      // build-info
      buildInfoKeys ++= Seq[BuildInfoKey](buildTime),
      buildInfoPackage := "inc.pyc",
      sourceGenerators in Compile <+= buildInfo,

      // eclipse
      EclipseKeys.withSource := true,

      publishMavenStyle := true,
            
      publishTo := Some(s3resolver.value(
          "My "+{if (isSnapshot.value) "snapshots-pyc-inc" else "releases-pyc-inc"}+" S3 bucket", 
          s3(if (isSnapshot.value) "snapshots-pyc-inc" else "releases-pyc-inc"))),
      
      s3credentials := {
        Path.userHome / ".ivy2" / ".s3credentials"
      },
      
      s3region := com.amazonaws.services.s3.model.Region.US_Standard 
    )
}

