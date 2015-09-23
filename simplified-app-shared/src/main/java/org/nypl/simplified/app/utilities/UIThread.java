package org.nypl.simplified.app.utilities;

import android.os.Handler;
import android.os.Looper;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Some trivial functions for asserting that code is running on the expected
 * thread.
 */

public final class UIThread
{
  private UIThread()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Check that the current thread is the UI thread and raise {@link
   * IllegalStateException} if it isn't.
   */

  public static void checkIsUIThread()
  {
    if (UIThread.isUIThread() == false) {
      throw new IllegalStateException(
        String.format(
          "Current thread '%s' is not the Android UI thread",
          Thread.currentThread()));
    }
  }

  /**
   * @return {@code true} iff the current thread is the UI thread.
   */

  public static boolean isUIThread()
  {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  /**
   * Run the given Runnable on the UI thread.
   *
   * @param r The runnable
   */

  public static void runOnUIThread(
    final Runnable r)
  {
    NullCheck.notNull(r);
    final Looper looper = NullCheck.notNull(Looper.getMainLooper());
    final Handler h = new Handler(looper);
    h.post(r);
  }

  /**
   * Run the given Runnable on the UI thread after the specified delay.
   *
   * @param r  The runnable
   * @param ms The delay in milliseconds
   */

  public static void runOnUIThreadDelayed(
    final Runnable r,
    final long ms)
  {
    NullCheck.notNull(r);
    final Looper looper = NullCheck.notNull(Looper.getMainLooper());
    final Handler h = new Handler(looper);
    h.postDelayed(r, ms);
  }
}
