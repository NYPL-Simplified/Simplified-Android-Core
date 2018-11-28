package org.nypl.simplified.app.utilities

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Functions to create thread pools.
 */

class NamedThreadPools {

  companion object {

    /**
     * Create a named thread pool.
     *
     * @param count The maximum number of threads in the pool
     * @param base The base name of the threads
     * @param priority The thread priority
     *
     * @return An executor service
     */

    fun namedThreadPool(
      count: Int,
      base: String,
      priority: Int): ExecutorService {

      val delegate = Executors.defaultThreadFactory()
      val named = object : ThreadFactory {
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

      return Executors.newFixedThreadPool(count, named)
    }

  }


}
