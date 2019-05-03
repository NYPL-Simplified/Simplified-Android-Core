package org.nypl.simplified.accounts.api;

import com.google.auto.value.AutoValue;

import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;

/**
 * <p>The set of Adobe credentials that are known after device activation.</p>
 * <p>These are received in OPDS feeds as licensor information. They are used to activate a device
 * and obtain a full set of credentials (such as the user ID and device ID).</p>
 *
 * @see AccountAuthenticationAdobePreActivationCredentials
 * @see org.nypl.drm.core.AdobeUserID
 * @see org.nypl.drm.core.AdobeDeviceID
 * @see org.nypl.simplified.opds.core.DRMLicensor
 */

@AutoValue
public abstract class AccountAuthenticationAdobePostActivationCredentials {

  AccountAuthenticationAdobePostActivationCredentials() {

  }

  /**
   * Create a set of credentials.
   *
   * @param device_id The activated device ID
   * @param user_id   The activated user ID
   * @return A set of credentials
   */

  public static AccountAuthenticationAdobePostActivationCredentials create(
      final AdobeDeviceID device_id,
      final AdobeUserID user_id) {
    return new AutoValue_AccountAuthenticationAdobePostActivationCredentials(device_id, user_id);
  }

  /**
   * @return The vendor ID
   */

  public abstract AdobeDeviceID deviceID();

  /**
   * @return The device token
   */

  public abstract AdobeUserID userID();
}
