package org.nypl.drm.core;

import java.util.concurrent.Future;

/**
 * The type of Join Account URI dispatchers.
 */

public interface AdobeAdeptJoinAccountDispatcherType
{
  /**
   * Submit a form URI.
   *
   * @param uri      The URI
   * @param listener The listener that will receive results
   *
   * @return A future representing the operation in progress
   */

  Future<Void> onFormSubmit(
    final String uri,
    final AdobeAdeptJoinAccountDispatcherListenerType listener);
}
