package org.nypl.simplified.ui.thread.api

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * An executor implementation that executes runnables on the UI thread and properly cancels
 * their execution when disposed.
 * It is primarily meant to safely listen to futures on the UI thread in a lifecycle-aware manner.
 *
 * Disposing the executor cancels the execution of any runnable previously submitted
 * and not yet executed, as well as that of runnables that might be submitted in the future.
 * Runnables being executed are not an issue because they are executed on the UI thread,
 * which means that no UI-related lifecycle action can be performed at the same time.
 */

class UIExecutor : Executor {

  private val handler: Handler =
    Handler(Looper.getMainLooper())

  private val callbacks: MutableList<Runnable> =
    mutableListOf()

  private var isDisposed: Boolean =
    false

  private val lock: Any =
    Any()

  override fun execute(command: Runnable) {
    synchronized(this.lock) {
      if (!this.isDisposed) {
        this.callbacks.add(command)
        this.handler.post(command)
      }
    }
  }

  /**
   * Cancel the execution of any runnable previously submitted
   * and not yet executed, as well as that of runnables that might be submitted in the future.
   */

  fun dispose() {
    synchronized(this.lock) {
      this.isDisposed = true
      while (this.callbacks.isNotEmpty()) {
        val callback = this.callbacks.removeFirst()
        this.handler.removeCallbacks(callback)
      }
    }
  }
}
