import $file.docs.generateDocs
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.github.lolgab::mill-mima::0.1.0`
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import com.goyeau.mill.scalafix.ScalafixModule
import mill._, scalalib._, publish._

val scalaVersions = Seq("2.13.12", "3.3.1")

trait Common extends CrossScalaModule with PublishModule with ScalafixModule{
  def scalaVersion = crossScalaVersion

  def publishVersion = VcsVersion.vcsState().format()

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/scalasql",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(
      owner = "com-lihaoyi",
      repo = "scalasql"
    ),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )

  def scalacOptions = Seq("-Xlint:unused")
}


object scalasql extends Cross[ScalaSql](scalaVersions)
trait ScalaSql extends Common{
  def moduleDeps = Seq(query, operations)
  def ivyDeps = Agg(
    ivy"org.apache.logging.log4j:log4j-api:2.20.0",
    ivy"org.apache.logging.log4j:log4j-core:2.20.0",
    ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.20.0",

  ) ++ Option.when(scalaVersion().startsWith("2."))(
    ivy"org.scala-lang:scala-reflect:$scalaVersion"
  )


  object test extends ScalaTests with ScalafixModule{
    def scalacOptions = Seq("-Xlint:unused")
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

    def forkArgs = Seq("-Duser.timezone=Asia/Singapore")
  }

  object core extends Common with CrossValue {
    def ivyDeps = Agg(
      ivy"com.lihaoyi::geny:1.0.0",
      ivy"com.lihaoyi::sourcecode:0.3.1",
      ivy"com.lihaoyi::pprint:0.8.1",
    ) ++ Option.when(scalaVersion().startsWith("2."))(
      ivy"org.scala-lang:scala-reflect:$scalaVersion"
    )

    def generatedSources = T {
      def commaSep0(i: Int, f: Int => String) = Range.inclusive(1, i).map(f).mkString(", ")


      val queryableRowDefs = for (i <- Range.inclusive(2, 22)) yield {
        def commaSep(f: Int => String) = commaSep0(i, f)
        s"""implicit def Tuple${i}Queryable[${commaSep(j => s"Q$j")}, ${commaSep(j => s"R$j")}](
           |    implicit
           |    ${commaSep(j => s"q$j: Queryable.Row[Q$j, R$j]")}
           |): Queryable.Row[(${commaSep(j => s"Q$j")}), (${commaSep(j => s"R$j")})] = {
           |  new Queryable.Row.TupleNQueryable(
           |    Seq(${commaSep(j => s"q$j.walkLabels()")}),
           |    t => Seq(${commaSep(j => s"q$j.walkExprs(t._$j)")}),
           |    construct0 = rsi => (${commaSep(j => s"q$j.construct(rsi)")}),
           |    deconstruct0 = { is => (${commaSep(j => s"""q$j.deconstruct(is._$j)""")}) }
           |  )
           |}""".stripMargin
      }


      os.write(
        T.dest / "Generated.scala",
        s"""package scalasql.core.generated
           |import scalasql.core.Queryable
           |trait QueryableRow{
           |  ${queryableRowDefs.mkString("\n")}
           |}
           |""".stripMargin
      )
      Seq(PathRef(T.dest / "Generated.scala"))
    }

  }


  object operations extends Common with CrossValue{
    def moduleDeps = Seq(core)
  }

  object query extends Common with CrossValue{
    def moduleDeps = Seq(core)


    def generatedSources = T {
      def commaSep0(i: Int, f: Int => String) = Range.inclusive(1, i).map(f).mkString(", ")

      def defs(isImpl: Boolean) = {
        for (i <- Range.inclusive(2, 22)) yield {
          def commaSep(f: Int => String) = commaSep0(i, f)

          val impl =
            if (!isImpl) ""
            else
              s"""= newInsertValues(
                 |        this,
                 |        columns = Seq(${commaSep(j => s"f$j(expr)")}),
                 |        valuesLists = items.map(t => Seq(${commaSep(j => s"t._$j")}))
                 |      )
                 |
                 |""".stripMargin
          s"""def batched[${commaSep(j => s"T$j")}](${commaSep(j => s"f$j: V[Column] => Column[T$j]")})(
             |    items: (${commaSep(j => s"Expr[T$j]")})*
             |)(implicit qr: Queryable[V[Column], R]): scalasql.query.InsertColumns[V, R] $impl""".stripMargin
        }
      }

      val queryableRowDefs = for (i <- Range.inclusive(2, 22)) yield {
        def commaSep(f: Int => String) = commaSep0(i, f)
        s"""implicit def Tuple${i}Queryable[${commaSep(j => s"Q$j")}, ${commaSep(j => s"R$j")}](
           |    implicit
           |    ${commaSep(j => s"q$j: Queryable.Row[Q$j, R$j]")}
           |): Queryable.Row[(${commaSep(j => s"Q$j")}), (${commaSep(j => s"R$j")})] = {
           |  import scalasql.core.SqlStr.SqlStringSyntax
           |  new Queryable.Row.TupleNQueryable(
           |    Seq(${commaSep(j => s"q$j.walkLabels()")}),
           |    t => Seq(${commaSep(j => s"q$j.walkExprs(t._$j)")}),
           |    construct0 = rsi => (${commaSep(j => s"q$j.construct(rsi)")}),
           |    deconstruct0 = { is => (${commaSep(j => s"""q$j.deconstruct(is._$j)""")}) }
           |  )
           |}""".stripMargin
      }

      val joinAppendDefs = for (i <- Range.inclusive(2, 21)) yield {
        def commaSep(f: Int => String) = commaSep0(i, f)

        val commaSepQ = commaSep(j => s"Q$j")
        val commaSepR = commaSep(j => s"R$j")
        val joinAppendType = s"scalasql.query.JoinAppend[($commaSepQ), QA, ($commaSepQ, QA), ($commaSepR, RA)]"
        s"""
           |implicit def append$i[$commaSepQ, QA, $commaSepR, RA](
           |      implicit qr0: Queryable.Row[($commaSepQ, QA), ($commaSepR, RA)],
           |      qr20: Queryable.Row[QA, RA]): $joinAppendType = new $joinAppendType {
           |    override def appendTuple(t: ($commaSepQ), v: QA): ($commaSepQ, QA) = (${commaSep(j => s"t._$j")}, v)
           |
           |    def qr: Queryable.Row[($commaSepQ, QA), ($commaSepR, RA)] = qr0
           |}""".stripMargin
      }

      os.write(
        T.dest / "Generated.scala",
        s"""package scalasql.generated
           |import scalasql.core.{Queryable, Expr}
           |import scalasql.query.Column
           |trait Insert[V[_[_]], R]{
           |  ${defs(false).mkString("\n")}
           |}
           |trait InsertImpl[V[_[_]], R] extends Insert[V, R]{ this: scalasql.query.Insert[V, R] =>
           |  def newInsertValues[R](
           |        insert: scalasql.query.Insert[V, R],
           |        columns: Seq[Column[_]],
           |        valuesLists: Seq[Seq[Expr[_]]]
           |    )(implicit qr: Queryable[V[Column], R]): scalasql.query.InsertColumns[V, R]
           |  ${defs(true).mkString("\n")}
           |}
           |
           |trait QueryableRow{
           |  ${queryableRowDefs.mkString("\n")}
           |}
           |
           |trait JoinAppend extends scalasql.query.JoinAppendLowPriority{
           |  ${joinAppendDefs.mkString("\n")}
           |}
           |""".stripMargin
      )
      Seq(PathRef(T.dest / "Generated.scala"))
    }
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
