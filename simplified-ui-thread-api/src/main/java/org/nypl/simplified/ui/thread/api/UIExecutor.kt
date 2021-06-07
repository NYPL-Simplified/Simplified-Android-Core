package org.nypl.simplified.ui.thread.api

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

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
