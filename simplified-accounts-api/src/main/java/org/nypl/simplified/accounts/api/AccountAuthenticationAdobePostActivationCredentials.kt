package org.nypl.simplified.accounts.api

import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID

/**
 *
 * The set of Adobe credentials that are known after device activation.
 *
 * These are received in OPDS feeds as licensor information. They are used to activate a device
 * and obtain a full set of credentials (such as the user ID and device ID).
 *
 * @see AccountAuthenticationAdobePreActivationCredentials
 * @see org.nypl.drm.core.AdobeUserID
 * @see org.nypl.drm.core.AdobeDeviceID
 */

data class AccountAuthenticationAdobePostActivationCredentials(

  /**
   * @return The vendor ID
   */

  val deviceID: AdobeDeviceID,

  /**
   * @return The device token
   */

  val userID: AdobeUserID
)
