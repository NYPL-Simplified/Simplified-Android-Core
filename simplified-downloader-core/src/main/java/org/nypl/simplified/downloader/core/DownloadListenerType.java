package org.nypl.simplified.downloader.core;

import java.io.File;
import java.io.IOException;

import com.io7m.jfunctional.OptionType;

/**
 * The type of download status listeners.
 */

public interface DownloadListenerType
{
  void onDownloadStarted(
    DownloadType d,
    long expected_total);

  void onDownloadDataReceived(
    DownloadType d,
    long running_total,
    long expected_total);

  void onDownloadCancelled(
    DownloadType d);

  void onDownloadFailed(
    DownloadType d,
    int status,
    long running_total,
    OptionType<Throwable> exception);

  void onDownloadCompleted(
    DownloadType d,
    File file)
    throws IOException;
}
