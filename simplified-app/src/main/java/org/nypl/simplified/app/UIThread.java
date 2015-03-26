package org.nypl.simplified.app;

import android.os.Handler;
import android.os.Looper;

import com.io7m.jnull.NullCheck;

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
    NullCheck.notNull(r);
    final Looper looper = NullCheck.notNull(Looper.getMainLooper());
    final Handler h = new Handler(looper);
    h.post(r);
  }
}
