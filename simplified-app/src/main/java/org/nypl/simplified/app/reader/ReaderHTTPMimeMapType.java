package org.nypl.simplified.app.reader;

/**
 * The type of maps from file suffixes to MIME types.
 */

public interface ReaderHTTPMimeMapType
{
  /**
   * @return The default MIME type
   */

  String getDefaultMimeType();

  /**
   * Retrieve the MIME type for a file with suffix <tt>suffix</tt>.
   *
   * @param suffix
   *          The file suffix
   * @return A MIME type
   */

  String getMimeTypeForSuffix(
    String suffix);
}
