package org.nypl.simplified.downloader.core;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.http.core.HTTPProblemReport;

import java.io.File;
import java.io.IOException;

/**
 * The type of download status listeners.
 */

public interface DownloadListenerType
{
  /**
   * A download has started.
   *
   * @param d              The download
   * @param expected_total The expected number of bytes
   */

  void onDownloadStarted(
    DownloadType d,
    long expected_total);

  /**
   * Data was received.
   *
   * @param d              The download
   * @param running_total  The current total
   * @param expected_total The expected total
   */

  void onDownloadDataReceived(
    DownloadType d,
    long running_total,
    long expected_total);

  /**
   * A download was cancelled.
   *
   * @param d The download
   */

  void onDownloadCancelled(
    DownloadType d);

  /**
   * A download failed.
   *
   * @param d             The download
   * @param status        The status code
   * @param running_total The number of bytes received
   * @param problemReport The HTTP problem report if one is available
   * @param exception     The exception raised, if any
   */

  void onDownloadFailed(
    DownloadType d,
    int status,
    long running_total,
    OptionType<HTTPProblemReport> problemReport,
    OptionType<Throwable> exception);

  /**
   * A download was completed
   *
   * @param d    The download
   * @param file The file
   *
   * @throws IOException If required
   */

  void onDownloadCompleted(
    DownloadType d,
    File file)
    throws IOException;
}
