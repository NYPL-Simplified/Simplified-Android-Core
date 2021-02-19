package org.nypl.drm.core;

/**
 * <p>The type of deactivation receivers.</p>
 */

public interface AdobeAdeptDeactivationReceiverType
{
  /**
   * Receive activation errors, if any.
   *
   * @param message The error message
   */

  void onDeactivationError(
    String message);

  /**
   * Called upon completion of deactivation, if no other errors have previously
   * occurred.
   */

  void onDeactivationSucceeded();
}
