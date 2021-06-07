package org.nypl.simplified.futures

import com.google.common.base.Function
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executor

/**
 * Extension functions to get around the lack of SAM conversion in Kotlin.
 */

object FluentFutureExtensions {

  /**
   * Apply a function `f` to the value of the current future.
   *
   * This is the same as `FluentFuture.transform` excepted that the compiler knows
   * that the result is not null.
   */

  fun <A, B> FluentFuture<A>.map(f: (A) -> B, executor: Executor): FluentFuture<B> {
    return this.transform(Function<A, B> { x -> f.invoke(x!!) }, executor)
  }

  /**
   * Apply a function `f` to the value of the current future.
   */

  fun <A, B> FluentFuture<A>.map(f: (A) -> B): FluentFuture<B> {
    return this.transform(Function<A, B> { x -> f.invoke(x!!) }, MoreExecutors.directExecutor())
  }

  /**
   * Apply a function `f` to the value of the current future, yielding a new future.
   */

  fun <A, B> FluentFuture<A>.flatMap(f: (A) -> FluentFuture<B>): FluentFuture<B> {
    return this.transformAsync(AsyncFunction<A, B> { x -> f.invoke(x!!) }, MoreExecutors.directExecutor())
  }

  /**
   * Apply a function `f` to an exception raised by the current future (if any).
   */

  fun <E : Throwable, A> FluentFuture<A>.onError(
    clazz: Class<E>,
    f: (E) -> A
  ): FluentFuture<A> {
    return this.catching(clazz, Function<E, A> { x -> f.invoke(x!!) }, MoreExecutors.directExecutor())
  }

  /**
   * Apply a function `f` to an exception raised by the current future (if any).
   */

  fun <E : Exception, A> FluentFuture<A>.onException(
    clazz: Class<E>,
    f: (E) -> A
  ): FluentFuture<A> {
    return this.catching(clazz, Function<E, A> { x -> f.invoke(x!!) }, MoreExecutors.directExecutor())
  }

  /**
   * Apply a function `f` to an exception raised by the current future (if any).
   */

  fun <A> FluentFuture<A>.onAnyError(f: (Throwable) -> A): FluentFuture<A> {
    return this.onError(Throwable::class.java, f)
  }

  /**
   * A future that completes when all of the given futures complete.
   */

  fun <A> fluentFutureOfAll(futures: List<FluentFuture<A>>) =
    FluentFuture.from(Futures.successfulAsList(futures))

  /**
   * A future that completes immediately with the given value.
   */

  fun <A> fluentFutureOfValue(x: A) =
    FluentFuture.from(Futures.immediateFuture(x))
}
