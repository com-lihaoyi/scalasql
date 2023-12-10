import collection.mutable
val generatedCodeHeader = "[//]: # (GENERATED SOURCES, DO NOT EDIT DIRECTLY)"
def generateTutorial(sourcePath: os.Path, destPath: os.Path) =  {
  var isDocs = Option.empty[Int]
  var isCode = false
  val outputLines = collection.mutable.Buffer.empty[String]
  outputLines.append(generatedCodeHeader)
  for (line <- os.read.lines(sourcePath)) {
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
  os.write.over(destPath, outputLines.mkString("\n"))
}
def generateReference(dest: os.Path, scalafmtCallback: (Seq[os.Path], os.Path) => Unit) =  {
  def dropDbPrefix(s: String) = s.split('.').drop(2).mkString(".")
  val records = upickle.default.read[Seq[Record]](os.read.stream(os.pwd / "out" / "recordedTests.json"))
  val suiteDescriptions = upickle.default.read[Map[String, String]](os.read.stream(os.pwd / "out" / "recordedSuiteDescriptions.json"))
    .map{case (k, v) => (dropDbPrefix(k), v)}

  val rawScalaStrs = records.flatMap(r => Seq(r.queryCodeString) ++ r.resultCodeString)
  val formattedScalaStrs = {
    val tmps = rawScalaStrs.map(os.temp(_, suffix = ".scala"))
    scalafmtCallback(tmps, os.pwd / ".scalafmt.conf")

    tmps.map(os.read(_).trim)
  }
  val scalafmt = rawScalaStrs.zip(formattedScalaStrs).toMap

  def dedent(s: String, newIndent: String) = {
    val indent = s
      .linesIterator
      .filter(_.trim.nonEmpty)
      .map(_.takeWhile(_ == ' ').size)
      .minOption
      .getOrElse(0)

    s
      .linesIterator
      .map(_.drop(indent))
      .mkString("\n" + newIndent)
      .trim
  }
  def sqlFormat(sOpt: Option[String]) = {
    sOpt match{
      case None => ""
      case Some(s) =>

        s"""
           |*
           |    ```sql
           |    ${dedent(s, "    ")}
           |    ```
           |""".stripMargin
    }
  }

  def renderResult(resultOpt: Option[String]) = {
    resultOpt match{
      case None => ""
      case Some(result) =>
        s"""
           |*
           |    ```scala
           |    ${scalafmt(result).linesIterator.mkString("\n    ")}
           |    ```
           |""".stripMargin
    }
  }


  val outputLines = mutable.Buffer.empty[String]
  outputLines.append(generatedCodeHeader)
  outputLines.append(
    s"""# ScalaSql Reference Library
       |
       |This page contains example queries for the ScalaSql, taken from the
       |ScalaSql test suite. You can use this as a reference to see what kinds
       |of operations ScalaSql supports and how these operations are translated
       |into raw SQL to be sent to the database for execution.
       |
       |If browsing this on Github, you can open the `Outline` pane on the right
       |to quickly browse through the headers, or use `Cmd-F` to search for specific
       |SQL keywords to see how to generate them from Scala (e.g. try searching for
       |`LEFT JOIN` or `WHEN`)
       |
       |Note that ScalaSql may generate different SQL in certain cases for different
       |databases, due to differences in how each database parses SQL. These differences
       |are typically minor, and as long as you use the right `Dialect` for your database
       |ScalaSql should do the right thing for you.
       |""".stripMargin
  )
  val recordsWithoutDuplicateSuites = records
    .groupBy(_.suiteName)
    .toSeq
    .sortBy(_._2.head.suiteLine)
    .distinctBy { case (k, v) => dropDbPrefix(k)}
    .map{case (k, vs) => (dropDbPrefix(k), vs.map(r => r.copy(suiteName = dropDbPrefix(r.suiteName))))}

  for((suiteName, suiteGroup) <- recordsWithoutDuplicateSuites) {
    val seen = mutable.Set.empty[String]
    outputLines.append(s"## $suiteName")
    outputLines.append(suiteDescriptions(suiteName))
    var lastSeen = ""
    for(r <- suiteGroup){

      val prettyName = (r.suiteName +: r.testPath).mkString(".")
      val titleOpt =
        if (prettyName == lastSeen) Some("----")
        else if (!seen(prettyName)) Some(s"### $prettyName")
        else None

      for(title <- titleOpt) {
        seen.add(prettyName)
        lastSeen = prettyName
        outputLines.append(
          s"""$title
             |
             |${dedent(r.docs, "")}
             |
             |```scala
             |${scalafmt(r.queryCodeString)}
             |```
             |
             |${sqlFormat(r.sqlString)}
             |
             |${renderResult(r.resultCodeString)}
             |
             |""".stripMargin
        )
      }
    }
  }
  os.write.over(dest, outputLines.mkString("\n"))
}




case class Record(suiteName: String,
                  suiteLine: Int,
                  testPath: Seq[String],
                  docs: String,
                  queryCodeString: String,
                  sqlString: Option[String],
                  resultCodeString: Option[String])

object Record {
  implicit val rw: upickle.default.ReadWriter[Record] = upickle.default.macroRW
}