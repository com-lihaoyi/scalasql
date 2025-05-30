package scalasql

import scalasql.UtestFramework.recordedTests

object UtestFramework {
  case class Record(
      suiteName: String,
      suiteLine: Int,
      testPath: Seq[String],
      docs: String,
      queryCodeString: String,
      sqlString: Option[String],
      resultCodeString: Option[String]
  )

  object Record {
    implicit val rw: upickle.default.ReadWriter[Record] = upickle.default.macroRW
  }
  val recordedTests = collection.mutable.Buffer.empty[Record]
  val recordedSuiteDescriptions = collection.mutable.Map.empty[String, String]
}
class UtestFramework extends utest.runner.Framework {
  override def setup() = {
    println("Setting up CustomFramework")
    recordedTests.clear()
  }
  override def teardown() = {
    println("Tearing down CustomFramework " + recordedTests.size)
    val workspaceRoot = os.Path(sys.env("MILL_WORKSPACE_ROOT"))
    val recordedTestsFile = os.RelPath(sys.env("SCALASQL_RECORDED_TESTS_NAME"))
    val recordedSuiteDescriptionsFile =
      os.RelPath(sys.env("SCALASQL_RECORDED_SUITE_DESCRIPTIONS_NAME"))
    os.write.over(
      workspaceRoot / "out" / recordedTestsFile,
      upickle.default.write(UtestFramework.recordedTests, indent = 4)
    )
    os.write.over(
      workspaceRoot / "out" / recordedSuiteDescriptionsFile,
      upickle.default.write(UtestFramework.recordedSuiteDescriptions, indent = 4)
    )
    recordedTests.clear()
  }

  override def exceptionStackFrameHighlighter(s: StackTraceElement): Boolean = {

    s.getClassName.contains("scalasql")
  }
}
