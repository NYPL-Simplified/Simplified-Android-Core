package org.nypl.simplified.accounts.api

import org.nypl.drm.core.AdobeVendorID
import java.net.URI

/**
 *
 * The set of Adobe credentials that are known prior to device activation.
 *
 * These are received in OPDS feeds as licensor information. They are used to activate a device
 * and obtain a full set of credentials (such as the user ID and device ID). The device manager
 * URI is contacted after activation.
 *
 * @see org.nypl.drm.core.AdobeUserID
 * @see org.nypl.drm.core.AdobeDeviceID
 */

data class AccountAuthenticationAdobePreActivationCredentials(

  /**
   * The vendor ID
   */

  val vendorID: AdobeVendorID,

  /**
   * The device token
   */

  val clientToken: AccountAuthenticationAdobeClientToken,

  /**
   * The device manager URI
   */

  val deviceManagerURI: URI?,

  /**
   * The post activation credentials that resulted from device activation, iff any device activation has been performed
   */

  val postActivationCredentials: AccountAuthenticationAdobePostActivationCredentials?)
