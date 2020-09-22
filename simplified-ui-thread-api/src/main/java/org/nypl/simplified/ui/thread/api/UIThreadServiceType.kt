package org.nypl.simplified.ui.thread.api

import android.os.Handler
import android.os.Looper

/**
 * A service to run functions on the Android UI thread.
 */

interface UIThreadServiceType {

  /**
   * Check that the current thread is the UI thread and raise [ ] if it isn't.
   */

  fun checkIsUIThread() {
    check(isUIThread() != false) {
      String.format(
        "Current thread '%s' is not the Android UI thread",
        Thread.currentThread()
      )
    }
  }

  /**
   * @return `true` iff the current thread is the UI thread.
   */

  fun isUIThread(): Boolean {
    return Looper.getMainLooper().thread === Thread.currentThread()
  }

  /**
   * Run the given Runnable on the UI thread.
   *
   * @param r The runnable
   */

  fun runOnUIThread(r: Runnable) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post(r)
  }

  /**
   * Run the given function on the UI thread.
   *
   * @param f The function
   */

  fun runOnUIThread(f: () -> Unit) =
    this.runOnUIThread(
      Runnable {
        f.invoke()
      }
    )

  /**
   * Run the given Runnable on the UI thread after the specified delay.
   *
   * @param r The runnable
   * @param ms The delay in milliseconds
   */

  fun runOnUIThreadDelayed(
    r: Runnable,
    ms: Long
  ) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.postDelayed(r, ms)
  }

  /**
   * Run the given function on the UI thread.
   *
   * @param f The function
   */

  fun runOnUIThreadDelayed(
    f: () -> Unit,
    ms: Long
  ) =
    this.runOnUIThreadDelayed(
      r = Runnable {
        f.invoke()
      },
      ms = ms
    )
}
