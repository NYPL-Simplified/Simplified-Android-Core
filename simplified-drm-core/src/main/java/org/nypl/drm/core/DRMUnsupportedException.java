package org.nypl.drm.core;

/**
 * A particular type of DRM is not supported.
 */

public final class DRMUnsupportedException extends DRMException
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception.
   *
   * @param message The exception message
   * @param e       The cause
   */

  public DRMUnsupportedException(
    final String message,
    final Throwable e)
  {
    super(message, e);
  }

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public DRMUnsupportedException(
    final String message)
  {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param e The cause
   */

  public DRMUnsupportedException(
    final Throwable e)
  {
    super(e);
  }
}
