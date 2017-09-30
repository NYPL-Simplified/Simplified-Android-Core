package org.nypl.simplified.app.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;

/**
 * Utility functions for showing error messages.
 */

public final class ErrorDialogUtilities
{
  private ErrorDialogUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Show an error dialog.
   *
   * @param ctx     The activity
   * @param log     The log handle
   * @param message The error message
   * @param x       The optional exception
   */

  public static void showError(
    final Activity ctx,
    final Logger log,
    final String message,
    final @Nullable Throwable x)
  {
    log.error("{}", message, x);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          final StringBuilder sb = new StringBuilder();
          sb.append(message);

          if (x != null) {
            sb.append("\n\n");
            sb.append(x);
          }

          final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
          b.setNeutralButton("OK", null);
          b.setMessage(NullCheck.notNull(sb.toString()));
          b.setTitle("Error");
          b.setCancelable(true);

          final AlertDialog a = b.create();
          a.show();
        }
      });
  }

  public static void showAlert(
    final Activity ctx,
    final Logger log,
    final String title,
    final String message,
    final @Nullable Throwable x)
  {
    log.error("{}", message, x);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          final StringBuilder sb = new StringBuilder();
          sb.append(message);

          if (x != null) {
            sb.append("\n\n");
            sb.append(x);
          }

          final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
          b.setNegativeButton("DISMISS", null);
          b.setMessage(NullCheck.notNull(sb.toString()));
          b.setTitle(title);
          b.setCancelable(true);

          final AlertDialog a = b.create();
          a.show();
        }
      });
  }

  /**
   * Show an error dialog, running the given runnable when the user dismisses
   * the message.
   *
   * @param ctx     The activity
   * @param log     The log handle
   * @param message The error message
   * @param x       The optional exception
   * @param r       The runnable to execute on dismissal
   */

  public static void showErrorWithRunnable(
    final Activity ctx,
    final Logger log,
    final String message,
    final @Nullable Throwable x,
    final Runnable r)
  {
    log.error("{}", message, x);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          final StringBuilder sb = new StringBuilder();
          sb.append(message);

          if (x != null) {
            sb.append("\n\n");
            sb.append(x);
          }

          final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
          b.setNeutralButton("OK", null);
          b.setMessage(NullCheck.notNull(sb.toString()));
          b.setTitle("Error");
          b.setCancelable(true);
          b.setOnDismissListener(
            new OnDismissListener()
            {
              @Override public void onDismiss(
                final @Nullable DialogInterface a)
              {
                r.run();
              }
            });

          final AlertDialog a = b.create();
          a.show();
        }
      });
  }
}
