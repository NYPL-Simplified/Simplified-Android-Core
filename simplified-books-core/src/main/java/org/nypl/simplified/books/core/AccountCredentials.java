package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.drm.core.AdobeVendorID;

/**
 * The type representing account credentials.
 */

public final class AccountCredentials
{
  private final AccountBarcode            user;
  private final AccountPIN                password;
  private final OptionType<AdobeVendorID> adobe_vendor;

  /**
   * Construct account credentials
   *
   * @param in_adobe_vendor The Adobe vendor ID that will be used to log into
   *                        the account. If no vendor ID is provided, no Adobe
   *                        login can occur.
   * @param in_user         The account username
   * @param in_password     The account password
   */

  public AccountCredentials(
    final OptionType<AdobeVendorID> in_adobe_vendor,
    final AccountBarcode in_user,
    final AccountPIN in_password)
  {
    this.adobe_vendor = NullCheck.notNull(in_adobe_vendor);
    this.user = NullCheck.notNull(in_user);
    this.password = NullCheck.notNull(in_password);
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("AccountCredentials{");
    sb.append("adobe_vendor=").append(this.adobe_vendor);
    sb.append(", user=").append(this.user);
    sb.append(", password=").append(this.password);
    sb.append('}');
    return sb.toString();
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final AccountCredentials that = (AccountCredentials) o;

    if (!this.getUser().equals(that.getUser())) {
      return false;
    }
    if (!this.getPassword().equals(that.getPassword())) {
      return false;
    }
    return this.adobe_vendor.equals(that.adobe_vendor);
  }

  @Override public int hashCode()
  {
    int result = this.getUser().hashCode();
    result = 31 * result + this.getPassword().hashCode();
    result = 31 * result + this.adobe_vendor.hashCode();
    return result;
  }

  /**
   * @return The Adobe vendor ID, if any
   */

  public OptionType<AdobeVendorID> getAdobeVendor()
  {
    return this.adobe_vendor;
  }

  /**
   * @return The account password
   */

  public AccountPIN getPassword()
  {
    return this.password;
  }

  /**
   * @return The account username
   */

  public AccountBarcode getUser()
  {
    return this.user;
  }
}
