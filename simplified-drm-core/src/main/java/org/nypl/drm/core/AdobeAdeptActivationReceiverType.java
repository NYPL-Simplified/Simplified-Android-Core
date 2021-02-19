package org.nypl.drm.core;

import com.io7m.jnull.Nullable;

/**
 * <p>The type of activation receivers.</p>
 *
 * @see AdobeAdeptConnectorType#getDeviceActivations
 * (AdobeAdeptActivationReceiverType)
 */

public interface AdobeAdeptActivationReceiverType
{
  /**
   * Receive the number of activations, if any.
   *
   * @param count The number of activations
   */

  void onActivationsCount(final int count);

  /**
   * Receive a specific activation.
   *
   * @param index     The index
   * @param authority The activation authority
   * @param device_id The device ID
   * @param user_name The user name
   * @param user_id   The user ID
   * @param expires   The expiry timestamp, if any
   */

  void onActivation(
    final int index,
    final AdobeVendorID authority,
    final AdobeDeviceID device_id,
    final String user_name,
    final AdobeUserID user_id,
    final @Nullable String expires);

  /**
   * Receive activation errors, if any.
   *
   * @param message The error message
   */

  void onActivationError(
    String message);
}
