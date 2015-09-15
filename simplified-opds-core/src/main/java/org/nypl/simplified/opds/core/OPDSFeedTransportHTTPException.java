package org.nypl.simplified.opds.core;

/**
 * The type of exceptions caused by HTTP errors.
 */

public final class OPDSFeedTransportHTTPException extends OPDSFeedTransportException
{
  private final int code;

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param in_code The HTTP status code
   */

  public OPDSFeedTransportHTTPException(
    final String message,
    final int in_code)
  {
    super(message);
    this.code = in_code;
  }

  /**
   * @return The status code
   */

  public int getCode()
  {
    return this.code;
  }
}
