import $file.buildutil.generateDocs
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.github.lolgab::mill-mima::0.1.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion

import mill._, scalalib._, publish._

val scalaVersions = Seq("2.13.12"/*, "3.3.1"*/)

object scalasql extends Cross[ScalaSql](scalaVersions)
trait ScalaSql extends CrossScalaModule with PublishModule{
  def scalaVersion = crossScalaVersion

  def publishVersion = VcsVersion.vcsState().format()

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/os-lib",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(
      owner = "com-lihaoyi",
      repo = "os-lib"
    ),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )

  def ivyDeps = Agg(
    ivy"com.lihaoyi::sourcecode:0.3.1",
    ivy"com.lihaoyi::upickle-implicits:3.1.3",

    ivy"org.apache.logging.log4j:log4j-api:2.20.0",
    ivy"org.apache.logging.log4j:log4j-core:2.20.0",
    ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.20.0",
    ivy"com.lihaoyi::pprint:0.8.1",
  ) ++ Option.when(scalaVersion().startsWith("2."))(
    ivy"org.scala-lang:scala-reflect:$scalaVersion"
  )

  def generatedSources: T[Seq[PathRef]] = T{
    def commaSep0(i: Int, f: Int => String) = Range.inclusive(1, i).map(f).mkString(", ")
    def defs(isImpl: Boolean) = {
      for(i <- Range.inclusive(2, 22)) yield {
        def commaSep(f: Int => String) = commaSep0(i, f)

        val impl =
          if (!isImpl) ""
          else s"""= newInsertValues(
                 |        this,
                 |        columns = Seq(${commaSep(j => s"f$j(expr)")}),
                 |        valuesLists = items.map(t => Seq(${commaSep(j => s"t._$j")}))
                 |      )
                 |
                 |""".stripMargin
        s"""def batched[${commaSep(j => s"T$j")}](${commaSep(j => s"f$j: Q => Column.ColumnExpr[T$j]")})(
          |    items: (${commaSep(j => s"Expr[T$j]")})*
          |)(implicit qr: Queryable[Q, R]): scalasql.query.InsertValues[Q, R] $impl""".stripMargin
      }
    }

    val queryableRowDefs = for(i <- Range.inclusive(2, 22)) yield {
      def commaSep(f: Int => String) = commaSep0(i, f)
      s"""implicit def Tuple${i}Queryable[${commaSep(j => s"Q$j")}, ${commaSep(j => s"R$j")}](
        |    implicit
        |    ${commaSep(j => s"q$j: Queryable.Row[Q$j, R$j]")}
        |): Queryable.Row[(${commaSep(j => s"Q$j")}), (${commaSep(j => s"R$j")})] = {
        |  new Queryable.Row.TupleNQueryable(
        |    t => Seq(${commaSep(j => s"q$j.walk(t._$j)")}),
        |    (q) => Seq(${commaSep(j => s"q$j.toTypeMappers(q._$j)")}),
        |    t => scalasql.utils.OptionPickler.Tuple${i}Reader(${commaSep(j => s"q$j.valueReader(t._$j)")})
        |  )
        |}""".stripMargin
    }

    os.write(
      T.dest / "Generated.scala",
      s"""package scalasql.generated
        |import scalasql.Column
        |import scalasql.Queryable
        |import scalasql.query.Expr
        |trait Insert[Q, R]{
        |  ${defs(false).mkString("\n")}
        |}
        |trait InsertImpl[Q, R] extends Insert[Q, R]{ this: scalasql.query.Insert[Q, R] =>
        |  def newInsertValues[Q, R](
        |        insert: scalasql.query.Insert[Q, R],
        |        columns: Seq[Column.ColumnExpr[_]],
        |        valuesLists: Seq[Seq[Expr[_]]]
        |    )(implicit qr: Queryable[Q, R]): scalasql.query.InsertValues[Q, R]
        |  ${defs(true).mkString("\n")}
        |}
        |
        |trait QueryableRow{
        |  ${queryableRowDefs.mkString("\n")}
        |}
        |""".stripMargin
    )
    Seq(PathRef(T.dest / "Generated.scala"))
  }

  object test extends ScalaTests {
    def ivyDeps = Agg(
      ivy"com.github.vertical-blank:sql-formatter:2.0.4",
      ivy"com.lihaoyi::mainargs:0.4.0",
      ivy"com.lihaoyi::os-lib:0.9.1",
      ivy"com.lihaoyi::upickle:3.1.3",
      ivy"com.lihaoyi::utest:0.8.2",
      ivy"com.h2database:h2:2.2.224",
      ivy"org.xerial:sqlite-jdbc:3.43.0.0",
      ivy"org.testcontainers:postgresql:1.19.1",
      ivy"org.postgresql:postgresql:42.6.0",
      ivy"org.testcontainers:mysql:1.19.1",
      ivy"mysql:mysql-connector-java:8.0.33",
      ivy"com.zaxxer:HikariCP:5.1.0"
    )

    def testFramework = "scalasql.UtestFramework"
  }
}

val generatedCodeHeader = "[//]: # (GENERATED SOURCES, DO NOT EDIT DIRECTLY)"
def generateTutorial() = T.command {
  generateDocs.generateTutorial(
    os.pwd / "scalasql" / "test" / "src" / "WorldSqlTests.scala",
    os.pwd / "docs" / "tutorial.md"
  )
}
def generateReference() = T.command {
  generateDocs.generateReference(
    os.pwd / "docs" / "reference.md",
    (sources, config) =>
      mill.scalalib.scalafmt.ScalafmtWorkerModule
        .worker()
        .reformat(sources.map(PathRef(_)), PathRef(config))
  )
}
