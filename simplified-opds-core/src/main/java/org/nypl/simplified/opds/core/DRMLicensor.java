package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.Serializable;


/**
 *
 */

public final class DRMLicensor implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final String vendor;
  private final String  client_token;

  /**
   * Construct an acquisition.
   *
   * @param in_vendor The vendor
   * @param in_client_token  The client token
   */

  public DRMLicensor(
    final String in_vendor,
    final String in_client_token)
  {
    this.vendor = NullCheck.notNull(in_vendor);
    this.client_token = NullCheck.notNull(in_client_token);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final DRMLicensor other = (DRMLicensor) obj;
    return (this.vendor.equals(other.vendor) && this.client_token.equals(other.client_token));
  }

  /**
   * @return The vendor
   */

  public String getVendor()
  {
    return this.vendor;
  }

  /**
   * @return The client token
   */

  public String getClientToken()
  {
    return this.client_token;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.vendor.hashCode();
    result = (prime * result) + this.client_token.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder builder = new StringBuilder(64);
    builder.append("[DRMLicensor ");
    builder.append(this.vendor);
    builder.append(" → ");
    builder.append(this.client_token);
    builder.append("]");
    return NullCheck.notNull(builder.toString());
  }

}
