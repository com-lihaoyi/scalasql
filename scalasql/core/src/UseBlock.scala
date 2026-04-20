package scalasql.core

/**
 * A block abstraction that wraps resource usage with proper acquiring and close/release.
 * Exposes lifecycle operations separately, which is useful for integration with FP effect libraries.
 * Should not be implemented outside of library code, so sealed.
 *
 * @tparam A The resource type provided during the `use` phase
 */
sealed trait UseBlock[+A] {

  /**
   * Acquires the resource. Called once at the start of the block.
   * @return The acquired resource and release function.
   */
  def allocate(): (A, Option[Throwable] => Unit)

  /**
   * Combines acquire, use, and release into a single operation.
   * Makes it friendly to traditional block-style API.
   */
  final def apply[T](use: A => T): T = {
    val (resource, release) = allocate()
    var usedOk = false
    try {
      val result = use(resource)
      usedOk = true
      release(None)
      result
    } catch {
      case e: Throwable =>
        if (!usedOk) {
          release(Some(e))
        } // else - we had an error in `release(None)`, so just propagating
        throw e
    }
  }
}

/** Package-private implementation for internal composition */
private[core] class UseBlockImpl[+A](alloc: () => (A, Option[Throwable] => Unit))
    extends UseBlock[A] {

  def allocate(): (A, Option[Throwable] => Unit) = alloc()

  def map[B](f: A => B): UseBlockImpl[B] = new UseBlockImpl[B](() => {
    val (a, release) = alloc()
    (f(a), release)
  })

  def flatMap[B](f: A => UseBlock[B]): UseBlockImpl[B] = new UseBlockImpl[B](() => {
    val (outerResource, outerRelease) = alloc()
    val (innerResource, innerRelease) =
      try {
        f(outerResource).allocate()
      } catch {
        case e: Throwable =>
          outerRelease(Some(e))
          throw e
      }
    def combinedRelease(errOpt: Option[Throwable]): Unit = {
      var errorForOuter = errOpt
      try {
        innerRelease(errOpt)
      } catch {
        case e: Throwable =>
          errorForOuter = Some(e)
          throw e
      } finally {
        outerRelease(errorForOuter)
      }
    }
    (innerResource, combinedRelease)
  })
}

private[core] object UseBlockImpl {

  def apply[A](acquire: => A)(release: (A, Option[Throwable]) => Unit): UseBlockImpl[A] =
    new UseBlockImpl[A](() => {
      val a = acquire
      (a, errOpt => release(a, errOpt))
    })

  def autoCloseable[A <: AutoCloseable](acquire: => A): UseBlockImpl[A] =
    apply(acquire)((a, _) => a.close())
}
