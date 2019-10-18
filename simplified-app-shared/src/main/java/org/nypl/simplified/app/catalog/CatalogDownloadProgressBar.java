package org.nypl.simplified.app.catalog;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Functions for configuring a progress bar indicating the status of a running
 * download.
 */

public final class CatalogDownloadProgressBar
{
  private CatalogDownloadProgressBar()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Set the progress bar.
   *
   * @param current_total  The current total bytes
   * @param expected_total The expected total bytes
   * @param text           The text
   * @param bar            The progress bar
   */

  public static void setProgressBar(
    final long current_total,
    final long expected_total,
    final TextView text,
    final ProgressBar bar)
  {
    NullCheck.notNull(text);
    NullCheck.notNull(bar);

    if (expected_total < 0L || current_total == 0L) {
      text.setText("");
      bar.setIndeterminate(true);
    } else {
      final double perc =
        ((double) current_total / (double) expected_total) * 100.0;
      final int iperc = (int) perc;
      bar.setIndeterminate(false);
      bar.setMax(100);
      bar.setProgress(iperc);
      text.setText(String.format("%d%%", Integer.valueOf(iperc)));
    }
  }
}
