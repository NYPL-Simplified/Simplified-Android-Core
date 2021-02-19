package org.nypl.drm.core;

import com.io7m.jnull.Nullable;

/**
 * The type of functions that map resources to byte arrays.
 */

public interface AdobeAdeptResourceProviderType
{
  /**
   * @param name The resource name
   *
   * @return The resource named <tt>name</tt> as an array of bytes, or
   * <tt>null</tt> if the resource does not exist.
   */

  @Nullable byte[] getResourceAsBytes(
    String name);
}
