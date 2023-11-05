package scalasql

import scalasql.UtestFramework.recorded

object UtestFramework {
  case class Record(
      suiteName: String,
      suiteLine: Int,
      testPath: Seq[String],
      queryCodeString: String,
      sqlString: Option[String],
      resultCodeString: Option[String]
  )

  object Record {
    implicit val rw: upickle.default.ReadWriter[Record] = upickle.default.macroRW
  }
  val recorded = collection.mutable.Buffer.empty[Record]
}
class UtestFramework extends utest.runner.Framework {
  override def setup() = {
    println("Setting up CustomFramework")
    recorded.clear()
  }
  override def teardown() = {
    println("Tearing down CustomFramework " + recorded.size)
    os.write.over(
      os.pwd / "recordedTests.json",
      upickle.default.write(UtestFramework.recorded, indent = 4)
    )
    recorded.clear()
  }

  override def exceptionStackFrameHighlighter(s: StackTraceElement): Boolean = {

    s.getClassName.contains("scalasql")
  }
}
