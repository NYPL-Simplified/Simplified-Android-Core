package org.nypl.simplified.app.catalog;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogDownloadProgressBar
{
  public static void setProgressBar(
    final long current_total,
    final long expected_total,
    final TextView text,
    final ProgressBar bar)
  {
    NullCheck.notNull(text);
    NullCheck.notNull(bar);

    if (expected_total < 0) {
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

  private CatalogDownloadProgressBar()
  {
    throw new UnreachableCodeException();
  }
}
