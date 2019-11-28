package org.nypl.simplified.viewer.epub.readium1;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.ui.thread.api.UIThreadServiceType;
import org.slf4j.Logger;

/**
 * Utility functions for showing error messages.
 */

public final class ErrorDialogUtilities {
  private ErrorDialogUtilities() {
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
    UIThreadServiceType uiThread,
    final Logger log,
    final String message,
    final @Nullable Throwable x) {
    log.error("{}", message, x);

    uiThread.runOnUIThread(
      () -> {
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
    UIThreadServiceType uiThread,
    final Logger log,
    final String message,
    final @Nullable Throwable x,
    final Runnable r) {
    log.error("{}", message, x);

    uiThread.runOnUIThread(
      () -> {
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
          a -> r.run());

        final AlertDialog a = b.create();
        a.show();
      });
  }
}
