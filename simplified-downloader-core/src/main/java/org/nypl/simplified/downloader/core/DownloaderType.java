package org.nypl.simplified.downloader.core;

import com.io7m.jfunctional.OptionType;
import org.nypl.simplified.http.core.HTTPAuthType;

import java.net.URI;

/**
 * The type of downloaders.
 */

public interface DownloaderType
{
  /**
   * Download the file at the given URI.
   *
   * @param uri      The URI
   * @param auth     The authentication data, if any
   * @param listener The listener
   *
   * @return A download
   */

  DownloadType download(
    URI uri,
    OptionType<HTTPAuthType> auth,
    DownloadListenerType listener);
}
