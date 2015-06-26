package org.nypl.simplified.app.drm;

/**
 * A particular type of DRM is not supported.
 */

final class DRMUnsupportedException extends DRMException
{
  private static final long serialVersionUID = 1L;

  DRMUnsupportedException(
    final String message,
    final Throwable e)
  {
    super(message, e);
  }

  DRMUnsupportedException(
    final String message)
  {
    super(message);
  }

  DRMUnsupportedException(
    final Throwable e)
  {
    super(e);
  }
}
