
import mill._, scalalib._

object scalasql extends RootModule with ScalaModule {
  def scalaVersion = "2.13.11"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode:0.3.1",
    ivy"com.lihaoyi::upickle-implicits:3.1.3",
    ivy"org.scala-lang:scala-reflect:$scalaVersion",
    ivy"org.apache.logging.log4j:log4j-api:2.20.0",
    ivy"org.apache.logging.log4j:log4j-core:2.20.0",
    ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.20.0",
  )

  object test extends ScalaTests {
    def ivyDeps = Agg(
      ivy"com.github.vertical-blank:sql-formatter:2.0.4",
      ivy"com.lihaoyi::mainargs:0.4.0",
      ivy"com.lihaoyi::os-lib:0.9.1",
      ivy"com.lihaoyi::upickle:3.1.3",
      ivy"com.lihaoyi::utest:0.8.1-18-79b46d",
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

  def generateDocs() = T.command {
    var isDocs = Option.empty[Int]
    var isCode = false
    val outputLines = collection.mutable.Buffer.empty[String]
    for (line <- os.read.lines(os.pwd / "test" / "src" / "WorldSqlTests.scala")) {
      val isDocsIndex = line.indexOf("// +DOCS")
      if (isDocsIndex != -1) {
        outputLines.append("")
        isDocs = Some(isDocsIndex)
        isCode = false
      }
      else if (line.contains("// -DOCS")) {
        isDocs = None
        if (isCode){
          outputLines.append("```")
          isCode = false
        }
      }
      else for (index <- isDocs) {
        val suffix = line.drop(index)
        (suffix, isCode) match{
          case ("", _) => outputLines.append("")

          case (s"// +INCLUDE $rest", _) =>
            os.read.lines(os.pwd / os.SubPath(rest)).foreach(outputLines.append)

          case (s"//$rest", false) => outputLines.append(rest.stripPrefix(" "))

          case (s"//$rest", true) =>
            outputLines.append("")
            outputLines.append("```")
            isCode = false
            outputLines.append(rest.stripPrefix(" "))

          case (c, true) => outputLines.append(c)

          case (c, false) =>
            outputLines.append("```scala")
            isCode = true
            outputLines.append(c)
        }

      }
    }
    os.write.over(os.pwd / "tutorial.md", outputLines.mkString("\n"))
  }
  def generateQueryLibrary() = T.command {
    def sqlFormat(s: String) = {
      if (s == null) ""
      else{

        val indent = s
          .linesIterator
          .filter(_.trim.nonEmpty)
          .map(_.takeWhile(_ == ' ').size)
          .minOption
          .getOrElse(0)

        val dedented = s
          .linesIterator
          .map(_.drop(indent))
          .mkString("\n")
          .trim
        s"""```sql
           |$dedented
           |```
           |""".stripMargin
      }
    }

    val records = upickle.default.read[Seq[Record]](os.read.stream(os.pwd / "recordedTests.json"))

    val rawScalaStrs = records.flatMap(r => Seq(r.queryCodeString, r.resultCodeString))
    val formattedScalaStrs = {
      val tmps = rawScalaStrs.map(os.temp(_, suffix = ".scala"))
      mill.scalalib.scalafmt.ScalafmtWorkerModule
        .worker()
        .reformat(tmps.map(PathRef(_)), PathRef(os.pwd / ".scalafmt.conf"))

      tmps.map(os.read(_).trim)
    }
    val scalafmt = rawScalaStrs.zip(formattedScalaStrs).toMap

    for ((dbName, dbGroup) <- records.groupBy(_.suiteName.split('.').apply(1))) {
      val outputLines = collection.mutable.Buffer.empty[String]
      outputLines.append(s"# $dbName")
      for((suiteName, suiteGroup) <- dbGroup.groupBy(_.suiteName).toSeq.sortBy(_._2.head.suiteLine)) {
        outputLines.append(s"## ${suiteName.split('.').drop(2).mkString(".")}")
        for(r <- suiteGroup){

          outputLines.append(
            s"""### ${(r.suiteName.split('.').drop(2) ++ r.testPath).mkString(".")}
               |
               |```scala
               |${scalafmt(r.queryCodeString)}
               |```
               |
               |${sqlFormat(r.sqlString)}
               |
               |```scala
               |${scalafmt(r.resultCodeString)}
               |```
               |
               |""".stripMargin
          )
        }
      }
      os.write.over(os.pwd / s"query-library-$dbName.md", outputLines.mkString("\n"))
    }
  }

}


case class Record(suiteName: String,
                  suiteLine: Int,
                  testPath: Seq[String],
                  queryCodeString: String,
                  sqlString: String,
                  resultCodeString: String)

object Record {
  implicit val rw: upickle.default.ReadWriter[Record] = upickle.default.macroRW
}