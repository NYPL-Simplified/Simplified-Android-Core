package org.nypl.simplified.downloader.core;

import java.io.IOException;

public final class DownloadAuthenticationError extends IOException
{
  private static final long serialVersionUID = 1L;

  public DownloadAuthenticationError(
    final String m)
  {
    super(m);
  }
}
