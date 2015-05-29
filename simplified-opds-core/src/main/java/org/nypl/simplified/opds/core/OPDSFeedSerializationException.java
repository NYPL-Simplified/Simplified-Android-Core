package org.nypl.simplified.opds.core;

import java.io.IOException;

/**
 * The type of errors raised during attempts to serialize OPDS feeds.
 */

public final class OPDSFeedSerializationException extends IOException
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception with no message or cause.
   */

  public OPDSFeedSerializationException()
  {
    super();
  }

  /**
   * Construct an exception.
   *
   * @param message
   *          The message
   */

  public OPDSFeedSerializationException(
    final String message)
  {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param message
   *          The message
   * @param cause
   *          The cause
   */

  public OPDSFeedSerializationException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct an exception
   *
   * @param cause
   *          The case
   */

  public OPDSFeedSerializationException(
    final Throwable cause)
  {
    super(cause);
  }
}
