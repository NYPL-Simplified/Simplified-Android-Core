package org.nypl.drm.core;

import java.net.URL;

//@formatter:off

/**
 * The type of listeners that receive the results of account joining
 * operations.
 *
 * @see AdobeAdeptConnectorType#joinAccount
 * (AdobeAdeptJoinAccountListenerType, AdobeUserID)
 */

//@formatter:on

public interface AdobeAdeptJoinAccountListenerType
{
  /**
   * Joining the accounts failed.
   *
   * @param error The error message
   */

  void onJoinAccountFailure(String error);

  /**
   * Called upon receipt of the initial join accounts URL. This URL should be
   * opened in a web view in order to allow the user to manually complete the
   * workflow.
   *
   * @param url The starting URL
   */

  void onJoinAccountStartingURL(URL url);
}
