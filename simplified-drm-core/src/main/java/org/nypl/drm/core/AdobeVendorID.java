package org.nypl.drm.core;

import java.util.Objects;

/**
 * <p>An Adobe vendor ID, typically in {@code urn:} form.</p> <p>For ordinary
 * Adobe IDs, this will be equal to {@code "AdobeID"}.</p>
 */

public final class AdobeVendorID
{
  private final String value;

  /**
   * Wrap a user ID.
   *
   * @param in_value The raw user ID.
   */

  public AdobeVendorID(
    final String in_value)
  {
    this.value = Objects.requireNonNull(in_value);
  }

  /**
   * @return The raw user ID value
   */

  public String getValue()
  {
    return this.value;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final AdobeVendorID that = (AdobeVendorID) o;
    return this.value.equals(that.value);
  }

  @Override public int hashCode()
  {
    return this.value.hashCode();
  }

  @Override public String toString()
  {
    return this.value;
  }
}
