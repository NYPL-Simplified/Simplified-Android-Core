package org.nypl.simplified.app.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * Utility functions for showing error messages.
 */

public final class ErrorDialogUtilities
{
  public static void showError(
    final Activity ctx,
    final String message,
    final @Nullable Throwable x)
  {
    Log.e("ERROR", message, x);

    UIThread.runOnUIThread(new Runnable() {
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

  public static void showErrorWithRunnable(
    final Activity ctx,
    final String message,
    final @Nullable Throwable x,
    final Runnable r)
  {
    Log.e("ERROR", message, x);

    UIThread.runOnUIThread(new Runnable() {
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
        b.setOnDismissListener(new OnDismissListener() {
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
