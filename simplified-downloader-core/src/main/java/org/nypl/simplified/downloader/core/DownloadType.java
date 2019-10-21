package org.nypl.simplified.downloader.core;

import java.net.URI;

/**
 * The type of a download in progress.
 */

public interface DownloadType
{
  /**
   * @return The URI of the download
   */

  URI uri();

  /**
   * Cancel the download in progress.
   */

  void cancel();

  /**
   * @return The content type of the remote data
   */

  String getContentType();
}
