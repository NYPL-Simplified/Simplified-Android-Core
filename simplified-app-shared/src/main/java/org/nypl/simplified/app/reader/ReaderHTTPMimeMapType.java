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
   * Attempt to extract a suffix from the URI {@code u} and then use {@link
   * #getMimeTypeForSuffix(String)} to attempt to guess the mime type.
   *
   * @param u The URI
   *
   * @return A mime type
   */

  String guessMimeTypeForURI(String u);

  /**
   * Retrieve the MIME type for a file with suffix {@code suffix}.
   *
   * @param suffix The file suffix
   *
   * @return A MIME type
   */

  String getMimeTypeForSuffix(
    String suffix);
}
