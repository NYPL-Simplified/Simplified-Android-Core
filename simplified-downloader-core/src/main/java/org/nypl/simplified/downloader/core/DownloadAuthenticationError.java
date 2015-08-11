package org.nypl.simplified.downloader.core;

import java.io.IOException;

/**
 * The type of download errors caused by authentication failures.
 */

public final class DownloadAuthenticationError extends IOException
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception.
   *
   * @param m The message
   */

  public DownloadAuthenticationError(
    final String m)
  {
    super(m);
  }
}
