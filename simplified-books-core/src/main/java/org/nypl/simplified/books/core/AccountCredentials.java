package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.opds.core.DRMLicensor;

/**
 * The type representing account credentials.
 */

public final class AccountCredentials {
  private OptionType<AdobeVendorID>   adobe_vendor;
  private final OptionType<AccountPatron>   patron;
  private AccountBarcode                    barcode;
  private AccountPIN                        pin;
  private OptionType<AccountAuthProvider>   provider;
  private OptionType<AccountAuthToken>      auth_token;
  private OptionType<AccountAdobeToken>     adobe_token;
  private OptionType<AdobeUserID>           user_id;
  private OptionType<AdobeDeviceID>         device_id;
  private OptionType<DRMLicensor>           licensor;


  /**
   * Construct account credentials
   *
   * @param in_adobe_vendor The Adobe vendor ID that will be used to log into
   *                        the account. If no vendor ID is provided, no Adobe
   *                        login can occur.
   * @param in_barcode      The account username
   * @param in_pin          The account password
   * @param in_provider     The account authentication provider
   */

  public AccountCredentials(
    final OptionType<AdobeVendorID> in_adobe_vendor,
    final AccountBarcode in_barcode,
    final AccountPIN in_pin,
    final OptionType<AccountAuthProvider> in_provider) {
    this.adobe_vendor = NullCheck.notNull(in_adobe_vendor);
    this.barcode = NullCheck.notNull(in_barcode);
    this.pin = NullCheck.notNull(in_pin);
    this.provider = NullCheck.notNull(in_provider);
    this.user_id = Option.none();
    this.device_id = Option.none();
    this.auth_token = Option.none();
    this.adobe_token = Option.none();
    this.patron = Option.none();
    this.licensor = Option.none();

  }

  /**
   * Construct account credentials
   *
   * @param in_adobe_vendor The Adobe vendor ID that will be used to log into
   *                        the account. If no vendor ID is provided, no Adobe
   *                        login can occur.
   * @param in_barcode      The account username
   * @param in_pin          The account password
   * @param in_provider     The account authentication provider
   * @param in_auth_token   The account authentication token
   * @param in_adobe_token  The adobe auth data token
   * @param in_patron       The account patron information
   */

  public AccountCredentials(
    final OptionType<AdobeVendorID> in_adobe_vendor,
    final AccountBarcode in_barcode,
    final AccountPIN in_pin,
    final OptionType<AccountAuthProvider> in_provider,
    final OptionType<AccountAuthToken> in_auth_token,
    final OptionType<AccountAdobeToken> in_adobe_token,
    final OptionType<AccountPatron> in_patron) {
    this.adobe_vendor = NullCheck.notNull(in_adobe_vendor);
    this.barcode = NullCheck.notNull(in_barcode);
    this.pin = NullCheck.notNull(in_pin);
    this.auth_token = NullCheck.notNull(in_auth_token);
    this.adobe_token = in_adobe_token;
    this.provider = in_provider;
    this.patron = in_patron;
    this.user_id = Option.none();
    this.device_id = Option.none();
    this.licensor = Option.none();

  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AccountCredentials{");
    sb.append("adobe_vendor=").append(this.adobe_vendor);
    sb.append(", barcode=").append(this.barcode);
    sb.append(", pin=").append(this.pin);
    sb.append(", adobe_token=").append(this.adobe_token);
    sb.append(", auth_token=").append(this.auth_token);
    sb.append(", user_id=").append(this.user_id);
    sb.append(", device_id=").append(this.device_id);
    sb.append(", provider=").append(this.provider);
    sb.append(", patron=").append(this.patron);
    sb.append(", licensor=").append(this.licensor);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final AccountCredentials that = (AccountCredentials) o;

    if (!this.getBarcode().equals(that.getBarcode())) {
      return false;
    }
    if (!this.getPin().equals(that.getPin())) {
      return false;
    }
    return this.adobe_vendor.equals(that.adobe_vendor);
  }

  @Override
  public int hashCode() {
    int result = this.getBarcode().hashCode();
    result = 31 * result + this.getPin().hashCode();
    result = 31 * result + this.adobe_vendor.hashCode();
    result = 31 * result + this.adobe_token.hashCode();
    result = 31 * result + this.auth_token.hashCode();
    result = 31 * result + this.user_id.hashCode();
    result = 31 * result + this.device_id.hashCode();
    result = 31 * result + this.provider.hashCode();
    result = 31 * result + this.patron.hashCode();
    result = 31 * result + this.licensor.hashCode();
    return result;
  }

  /**
   * @return The Adobe vendor ID, if any
   */

  public OptionType<AdobeVendorID> getAdobeVendor() {
    return this.adobe_vendor;
  }

  /**
   * @return drm licensor
   */
  public OptionType<DRMLicensor> getDrmLicensor() {
    return this.licensor;
  }

  /**
   * @return The account password
   */

  public AccountPIN getPin() {
    return this.pin;
  }

  /**
   * @return The account username
   */

  public AccountBarcode getBarcode() {
    return this.barcode;
  }

  /**
   * @return The aut token
   */

  public OptionType<AccountAuthToken> getAuthToken() {
    return this.auth_token;
  }

  /**
   * @param in_auth_token token for registration and deregistration
   */
  public void setAuthToken(final OptionType<AccountAuthToken> in_auth_token) {
    this.auth_token = in_auth_token;
  }

  /**
   * @return The adobe token
   */

  public OptionType<AccountAdobeToken> getAdobeToken() {
    return this.adobe_token;
  }

  /**
   * @param in_adobe_token token for registration and deregistration
   */
  public void setAdobeToken(final OptionType<AccountAdobeToken> in_adobe_token) {
    this.adobe_token = in_adobe_token;
  }

  /**
   * @param in_adobe_vendor vendor for registration and deregistration
   */
  public void setAdobeVendor(final OptionType<AdobeVendorID> in_adobe_vendor) {
    this.adobe_vendor = in_adobe_vendor;
  }

  /**
   * @param in_drm_licensor drm licensor
   */
  public void setDrmLicensor(final OptionType<DRMLicensor> in_drm_licensor) {
    this.licensor = in_drm_licensor;
  }

  /**
   * @return The adobe user id
   */

  public OptionType<AdobeUserID> getAdobeUserID() {
    return this.user_id;
  }

  /**
   * @param in_user_id user id used by adobe to register/deregister a device
   */
  public void setAdobeUserID(final OptionType<AdobeUserID> in_user_id) {
    this.user_id = in_user_id;
  }

  /**
   * @return The adobe device id
   */

  public OptionType<AdobeDeviceID> getAdobeDeviceID() {
    return this.device_id;
  }

  /**
   * @param in_device_id device id used by adobe to register/deregister a device
   */
  public void setAdobeDeviceID(final OptionType<AdobeDeviceID> in_device_id) {
    this.device_id = in_device_id;
  }

  /**
   * @return The authentication provider
   */

  public OptionType<AccountAuthProvider> getProvider() {
    return this.provider;
  }

  /**
   * @param in_provider provider name
   */
  public void setProvider(final OptionType<AccountAuthProvider> in_provider) {
    this.provider = in_provider;
  }

  /**
   * @return The patron information
   */

  public OptionType<AccountPatron> getPatron() {
    return this.patron;
  }

  /**
   * @param in_pin library card pin
   */
  public void setAdobePIN(final AccountPIN in_pin) {
    this.pin = in_pin;
  }

  /**
   * @param in_barcode library card barcode
   */
  public void setUser(final AccountBarcode in_barcode) {
    this.barcode = in_barcode;
  }
}
