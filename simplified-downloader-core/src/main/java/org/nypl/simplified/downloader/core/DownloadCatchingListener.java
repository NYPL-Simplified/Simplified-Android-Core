package org.nypl.simplified.downloader.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.http.core.HTTPProblemReport;
import org.slf4j.Logger;

import java.io.File;

/**
 * An implementation of the {@link DownloadListenerType} interface that
 * delegates to an existing implementation and catches and logs all unchecked
 * exceptions raised.
 */

public final class DownloadCatchingListener implements DownloadListenerType
{
  private final DownloadListenerType listener;
  private final Logger               log;

  /**
   * Construct a listener.
   *
   * @param in_log      A log interface
   * @param in_listener A delegate
   */

  public DownloadCatchingListener(
    final Logger in_log,
    final DownloadListenerType in_listener)
  {
    this.log = NullCheck.notNull(in_log);
    this.listener = NullCheck.notNull(in_listener);
  }

  @Override public void onDownloadStarted(
    final DownloadType d,
    final long in_expected)
  {
    try {
      this.listener.onDownloadStarted(d, in_expected);
    } catch (final Throwable x) {
      this.log.error(
        "Ignoring exception: onDownloadStarted raised: ", x);
    }
  }

  @Override public void onDownloadFailed(
    final DownloadType d,
    final int in_status,
    final long in_running_total,
    final OptionType<HTTPProblemReport> problemReport,
    final OptionType<Throwable> in_exception)
  {
    try {
      this.listener.onDownloadFailed(
        d, in_status, in_running_total, problemReport, in_exception);
    } catch (final Throwable x) {
      this.log.error(
        "Ignoring exception: onDownloadFailed raised: ", x);
    }
  }

  @Override public void onDownloadDataReceived(
    final DownloadType d,
    final long in_running_total,
    final long in_expected_total)
  {
    try {
      this.listener.onDownloadDataReceived(
        d, in_running_total, in_expected_total);
    } catch (final Throwable x) {
      this.log.error(
        "Ignoring exception: onDownloadDataReceived raised: ", x);
    }
  }

  @Override public void onDownloadCompleted(
    final DownloadType d,
    final File in_file)
  {
    try {
      this.listener.onDownloadCompleted(d, in_file);
    } catch (final Throwable x) {
      this.log.error(
        "Ignoring exception: onDownloadCompleted raised: ", x);
    }
  }

  @Override public void onDownloadCancelled(
    final DownloadType d)
  {
    try {
      this.listener.onDownloadCancelled(d);
    } catch (final Throwable x) {
      this.log.error(
        "Ignoring exception: onDownloadCancelled raised: ", x);
    }
  }
}
