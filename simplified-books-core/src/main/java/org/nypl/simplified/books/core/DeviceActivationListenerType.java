package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account sync operations.
 *
 * If authentication fails,
 * is called. Otherwise, for each book in the account  or is called, followed by . Otherwise,  is called.
 */

public interface DeviceActivationListenerType
{

  /**
   * activation failed.
   *
   * @param message The error message
   */

  void onDeviceActivationFailure(
    String message);

  /**
   * activation finished successfully.
   */

  void onDeviceActivationSuccess();

}
