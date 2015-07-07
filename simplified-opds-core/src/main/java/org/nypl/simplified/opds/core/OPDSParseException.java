package org.nypl.simplified.opds.core;

import java.io.IOException;

/**
 * The type of errors raised during attempts to parse OPDS feeds.
 */

public final class OPDSParseException extends IOException
{
  private static final long serialVersionUID = 5463756509505950662L;

  /**
   * Construct an exception with no message or cause.
   */

  public OPDSParseException()
  {
    super();
  }

  /**
   * Construct an exception.
   *
   * @param message
   *          The message
   */

  public OPDSParseException(
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

  public OPDSParseException(
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

  public OPDSParseException(
    final Throwable cause)
  {
    super(cause);
  }
}
