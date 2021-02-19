package org.nypl.drm.core;

/**
 * An exception caused by attempting to process an ACSM file that is in some way
 * invalid.
 */

public class AdobeAdeptACSMException extends Exception
{
  /**
   * Construct an exception.
   *
   * @param message The message
   */

  public AdobeAdeptACSMException(final String message)
  {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param cause   The cause
   */

  public AdobeAdeptACSMException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public AdobeAdeptACSMException(final Throwable cause)
  {
    super(cause);
  }
}
