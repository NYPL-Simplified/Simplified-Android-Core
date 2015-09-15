package org.nypl.simplified.opds.core;

/**
 * The type of exceptions raised by feed transports.
 */

public abstract class OPDSFeedTransportException extends Exception
{
  /**
   * Construct an exception.
   *
   * @param message The message
   * @param cause   The cause
   */

  public OPDSFeedTransportException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct an exception.
   *
   * @param message The message
   */

  public OPDSFeedTransportException(
    final String message)
  {
    super(message);
  }
}
