package org.nypl.simplified.accounts.api;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.nypl.drm.core.AdobeVendorID;

import java.net.URI;

/**
 * <p>The set of Adobe credentials that are known prior to device activation.</p>
 * <p>These are received in OPDS feeds as licensor information. They are used to activate a device
 * and obtain a full set of credentials (such as the user ID and device ID). The device manager
 * URI is contacted after activation.</p>
 *
 * @see org.nypl.drm.core.AdobeUserID
 * @see org.nypl.drm.core.AdobeDeviceID
 * @see org.nypl.simplified.opds.core.DRMLicensor
 */

@AutoValue
public abstract class AccountAuthenticationAdobePreActivationCredentials {

  AccountAuthenticationAdobePreActivationCredentials() {

  }

  /**
   * Create a set of credentials.
   *
   * @param vendor_id The vendor ID
   * @param token     The client token
   * @param uri       The device manager URI
   * @param post      The post activation credentials that resulted from device activation, if any device activation has been performed
   * @return A set of credentials
   */

  public static AccountAuthenticationAdobePreActivationCredentials create(
      final AdobeVendorID vendor_id,
      final AccountAuthenticationAdobeClientToken token,
      final URI uri,
      final OptionType<AccountAuthenticationAdobePostActivationCredentials> post) {
    return new AutoValue_AccountAuthenticationAdobePreActivationCredentials(vendor_id, token, uri, post);
  }

  /**
   * @return The vendor ID
   */

  public abstract AdobeVendorID vendorID();

  /**
   * @return The device token
   */

  public abstract AccountAuthenticationAdobeClientToken clientToken();

  /**
   * @return The device manager URI
   */

  public abstract URI deviceManagerURI();

  /**
   * @return The post activation credentials that resulted from device activation, iff any device activation has been performed
   */

  public abstract OptionType<AccountAuthenticationAdobePostActivationCredentials> postActivationCredentials();

  /**
   * @param credentials The extra post-activation credentials
   * @return The current credentials plus the given post-activation credentials
   */

  public AccountAuthenticationAdobePreActivationCredentials withPostActivationCredentials(
      final AccountAuthenticationAdobePostActivationCredentials credentials) {
    return create(
        this.vendorID(),
        this.clientToken(),
        this.deviceManagerURI(),
        Option.some(credentials));
  }

  /**
   * @return The current credentials without any existing post-activation credentials
   */

  public AccountAuthenticationAdobePreActivationCredentials withoutPostActivationCredentials() {
    return create(
        this.vendorID(),
        this.clientToken(),
        this.deviceManagerURI(),
        Option.<AccountAuthenticationAdobePostActivationCredentials>none());
  }
}
