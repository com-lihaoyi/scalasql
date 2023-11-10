import $file.generateDocs
import mill._
import scalalib._

import scala.collection.mutable

object scalasql extends RootModule with ScalaModule {
  def scalaVersion = "2.13.11"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode:0.3.1",
    ivy"com.lihaoyi::upickle-implicits:3.1.3",
    ivy"org.scala-lang:scala-reflect:$scalaVersion",
    ivy"org.apache.logging.log4j:log4j-api:2.20.0",
    ivy"org.apache.logging.log4j:log4j-core:2.20.0",
    ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.20.0",
    ivy"com.lihaoyi::pprint:0.8.1",
  )

  object test extends ScalaTests {
    def ivyDeps = Agg(
      ivy"com.github.vertical-blank:sql-formatter:2.0.4",
      ivy"com.lihaoyi::mainargs:0.4.0",
      ivy"com.lihaoyi::os-lib:0.9.1",
      ivy"com.lihaoyi::upickle:3.1.3",
      ivy"com.lihaoyi::utest:0.8.2",
      ivy"com.h2database:h2:2.2.224",
      ivy"org.hsqldb:hsqldb:2.5.1",
      ivy"org.xerial:sqlite-jdbc:3.43.0.0",
      ivy"org.testcontainers:postgresql:1.19.1",
      ivy"org.postgresql:postgresql:42.6.0",
      ivy"org.testcontainers:mysql:1.19.1",
      ivy"mysql:mysql-connector-java:8.0.33",
    )

    def testFramework = "scalasql.UtestFramework"
  }

  val generatedCodeHeader = "[//]: # (GENERATED SOURCES, DO NOT EDIT DIRECTLY)"
  def generateTutorial() = T.command {
    generateDocs.generateTutorial()
  }
  def generateReference() = T.command {
    generateDocs.generateReference((sources, config) =>
      mill.scalalib.scalafmt.ScalafmtWorkerModule
        .worker()
        .reformat(sources.map(PathRef(_)), PathRef(config))
    )
  }
}