package org.nypl.drm.core;

/**
 * The root type of DRM-related exceptions.
 */

public abstract class DRMException extends Exception
{
  private static final long serialVersionUID = 1L;

  DRMException(
    final String message,
    final Throwable e)
  {
    super(message, e);
  }

  DRMException(
    final String message)
  {
    super(message);
  }

  DRMException(
    final Throwable e)
  {
    super(e);
  }
}
