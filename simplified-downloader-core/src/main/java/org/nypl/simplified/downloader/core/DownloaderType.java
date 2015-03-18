package org.nypl.simplified.downloader.core;

import java.net.URI;
import java.util.Map;

import com.io7m.jfunctional.OptionType;

/**
 * The type of download managers.
 */

public interface DownloaderType
{
  /**
   * Acknowledge a completed download, removing it from the downloader. Has no
   * effect if the download is still in progress, or if the download does not
   * exist.
   *
   * @param id
   *          The id of the download
   */

  void downloadAcknowledge(
    final long id);

  /**
   * Cancel the given download. Has no effect if the download is already
   * completed, or does not exist.
   *
   * @param id
   *          The unique identifier of the download in progress
   */

  void downloadCancel(
    long id);

  /**
   * Halt all downloads and delete any data.
   */

  void downloadDestroyAll();

  /**
   * Queue a URI for downloading.
   *
   * @param uri
   *          The URI
   * @param title
   *          A human-readable name for the download
   * @return The unique identifier of the download in progress
   */

  long downloadEnqueue(
    final URI uri,
    final String title);

  /**
   * Pause the given download. Has no effect if the download is already
   * completed, or does not exist.
   *
   * @param id
   *          The unique identifier of the download in progress
   */

  void downloadPause(
    long id);

  /**
   * Resume the given download. Has no effect if the download is not paused,
   * or does not exist.
   *
   * @param id
   *          The unique identifier of the download in progress
   */

  void downloadResume(
    long id);

  /**
   * @return A snapshot of the given download
   */

  OptionType<DownloadSnapshot> downloadStatusSnapshot(
    long id);

  /**
   * @return A snapshot of all the current downloads
   */

  Map<Long, DownloadSnapshot> downloadStatusSnapshotAll();
}
