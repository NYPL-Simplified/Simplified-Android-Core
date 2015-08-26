package org.nypl.simplified.opds.core;

import java.io.IOException;

/**
 * The type of errors raised during attempts to serialize OPDS objects.
 */

public final class OPDSSerializationException extends IOException
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception with no message or cause.
   */

  public OPDSSerializationException()
  {
    super();
  }

  /**
   * Construct an exception.
   *
   * @param message
   *          The message
   */

  public OPDSSerializationException(
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

  public OPDSSerializationException(
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

  public OPDSSerializationException(
    final Throwable cause)
  {
    super(cause);
  }
}
