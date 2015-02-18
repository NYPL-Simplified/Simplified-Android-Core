package org.nypl.simplified.app;

import android.os.Handler;
import android.os.Looper;

/**
 * Some trivial functions for asserting that code is running on the expected
 * thread.
 */

final class UIThread
{
  /**
   * Check that the current thread is the UI thread and raise
   * {@link IllegalStateException} if it isn't.
   */

  static void checkIsUIThread()
  {
    if (UIThread.isUIThread() == false) {
      throw new IllegalStateException(String.format(
        "Current thread '%s' is not the Android UI thread",
        Thread.currentThread()));
    }
  }

  /**
   * @return <code>true</code> iff the current thread is the UI thread.
   */

  static boolean isUIThread()
  {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  /**
   * Run the given Runnable on the UI thread.
   * 
   * @param r
   *          The runnable
   */

  static void runOnUIThread(
    final Runnable r)
  {
    new Handler(Looper.getMainLooper()).post(r);
  }
}
