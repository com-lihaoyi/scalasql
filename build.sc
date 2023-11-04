
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
      ivy"com.lihaoyi::utest:0.7.11",
      ivy"com.h2database:h2:2.2.224",
      ivy"org.hsqldb:hsqldb:2.5.1",
      ivy"org.xerial:sqlite-jdbc:3.43.0.0",
      ivy"org.testcontainers:postgresql:1.19.1",
      ivy"org.postgresql:postgresql:42.6.0",
      ivy"org.testcontainers:mysql:1.19.1",
      ivy"mysql:mysql-connector-java:8.0.33",
    )

    def testFramework = "utest.runner.Framework"
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

}

