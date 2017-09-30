package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;


/**
 *
 */

public final class DRMLicensor implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final OptionType<String> vendor;
  private final OptionType<String> client_token;
  private final OptionType<String> client_token_url;
  private final OptionType<String> device_manager;
  private final DRM drm_type;

  /**
   * Construct an acquisition.
   *  @param in_vendor The vendor
   * @param in_client_token  The client token
   * @param in_client_token_url urms client token url
   * @param in_device_manager  The device manager url
   * @param in_drm_type    The DRM type (ADOBE, URMS, LCP ...)
   */

  public DRMLicensor(
    final OptionType<String> in_vendor,
    final OptionType<String> in_client_token,
    final OptionType<String> in_client_token_url,
    final OptionType<String> in_device_manager,
    final DRM in_drm_type)
  {
    this.vendor = NullCheck.notNull(in_vendor);
    this.client_token = NullCheck.notNull(in_client_token);
    this.client_token_url = NullCheck.notNull(in_client_token_url);
    this.device_manager = NullCheck.notNull(in_device_manager);
    this.drm_type = in_drm_type;
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
    return (this.vendor.equals(other.vendor)
      && this.client_token.equals(other.client_token)
      && this.client_token_url.equals(other.client_token_url)
      && this.device_manager.equals(other.device_manager));
  }

  /**
   * @return The vendor
   */

  public OptionType<String> getVendor()
  {
    return this.vendor;
  }

  /**
   * @return The client token
   */

  public OptionType<String> getClientToken()
  {
    return this.client_token;
  }

  /**
   * @return client token url
   */
  public OptionType<String> getClientTokenUrl()
  {
    return this.client_token_url;
  }

  /**
   * @return The devie manager url
   */
  public OptionType<String> getDeviceManager()
  {
    return this.device_manager;
  }

  /**
   * @return drm type
   */
  public DRM getDrmType() {
    return this.drm_type;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.vendor.hashCode();
    result = (prime * result) + this.client_token.hashCode();
    result = (prime * result) + this.client_token_url.hashCode();
    result = (prime * result) + this.device_manager.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final AtomicReference<StringBuilder> builder = new AtomicReference<StringBuilder>(new StringBuilder(64));
    builder.get().append("[DRMLicensor ");
    builder.get().append(this.vendor);
    builder.get().append(" → ");
    builder.get().append(this.client_token);
    builder.get().append(" → ");
    builder.get().append(this.client_token_url);
    builder.get().append(" → ");
    builder.get().append(this.device_manager);
    builder.get().append("]");
    return NullCheck.notNull(builder.get().toString());
  }
  /**
   *
   */
  public enum DRM {
    /**
     *
     */
    NONE,
    /**
     *
     */
    ADOBE,
    /**
     *
     */
    URMS,
    /**
     *
     */
    LCP
  }

}
