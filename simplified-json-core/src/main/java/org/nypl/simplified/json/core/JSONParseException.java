package org.nypl.simplified.json.core;

import java.io.IOException;

/**
 * The type of errors raised during attempts to parse JSON data.
 */

public final class JSONParseException extends IOException
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception with no message or cause.
   */

  public JSONParseException()
  {
    super();
  }

  /**
   * Construct an exception.
   *
   * @param message
   *          The message
   */

  public JSONParseException(
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

  public JSONParseException(
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

  public JSONParseException(
    final Throwable cause)
  {
    super(cause);
  }
}
