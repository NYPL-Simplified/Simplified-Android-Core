package org.nypl.simplified.app.catalog;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogDownloadProgressBar
{
  public static void setProgressBar(
    final DownloadSnapshot snap,
    final TextView text,
    final ProgressBar bar)
  {
    NullCheck.notNull(snap);
    NullCheck.notNull(text);
    NullCheck.notNull(bar);

    final long max = snap.statusGetMaximumBytes();
    final long cur = snap.statusGetCurrentBytes();
    if (max < 0) {
      bar.setIndeterminate(true);
    } else {
      final double perc = ((double) cur / (double) max) * 100.0;
      final int iperc = (int) perc;
      bar.setIndeterminate(false);
      bar.setMax(100);
      bar.setProgress(iperc);
      text.setText(String.format("%d%%", iperc));
    }
  }

  private CatalogDownloadProgressBar()
  {
    throw new UnreachableCodeException();
  }
}
