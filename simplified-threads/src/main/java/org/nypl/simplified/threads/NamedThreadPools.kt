package org.nypl.simplified.threads

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Functions to create thread pools.
 */

object NamedThreadPools {

  /**
   * Create a named thread pool.
   *
   * @param count The maximum number of threads in the pool
   * @param base The base name of the threads
   * @param priority The thread priority
   *
   * @return An executor service
   */

  @JvmStatic
  fun namedThreadPool(
    count: Int,
    base: String,
    priority: Int
  ): ListeningScheduledExecutorService =
    this.namedThreadPoolOf(count, this.namedThreadPoolFactory(base, priority))

  @JvmStatic
  fun namedThreadPoolOf(
    count: Int,
    factory: ThreadFactory
  ): ListeningScheduledExecutorService =
    MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(count, factory))

  @JvmStatic
  fun namedThreadPoolFactory(
    base: String,
    priority: Int
  ): ThreadFactory {
    val delegate = Executors.defaultThreadFactory()
    return object : ThreadFactory {
      private var id: Int = 0

      override fun newThread(runnable: Runnable): Thread {
        val t = delegate.newThread {
          android.os.Process.setThreadPriority(priority)
          runnable.run()
        }
        t.name = String.format("simplified-%s-tasks-%d", base, this.id)
        ++this.id
        return t
      }
    }
  }
}
