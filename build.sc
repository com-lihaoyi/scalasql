
import mill._, scalalib._

object usql extends RootModule with ScalaModule {
  def scalaVersion = "2.13.11"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::mainargs:0.4.0",
    ivy"com.lihaoyi::sourcecode:0.3.1",
    ivy"com.lihaoyi::upickle:3.1.3",
    ivy"com.lihaoyi::os-lib:0.9.1",
  )

  object test extends ScalaTests {
    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest:0.7.11",
      ivy"com.h2database:h2:2.2.224"
    )
    def testFramework = "utest.runner.Framework"
  }
}
