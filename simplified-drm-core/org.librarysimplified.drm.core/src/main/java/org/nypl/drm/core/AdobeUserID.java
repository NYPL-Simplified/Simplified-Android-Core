package org.nypl.drm.core;

import java.util.Objects;

/**
 * <p>An Adobe user ID, typically in {@code urn:} form. This is <i>not</i> an
 * "Adobe ID", but is the internal representation that Adobe use to represent
 * user identifiers. These are exposed to the programmer via on-disk device
 * activation records, or when attempting to join accounts.</p>
 *
 * <p>This type exists to reduce the "everything is a string" aspect of the
 * native code API, and therefore to reduce any possibility of accidentally
 * mixing up values when calling functions.</p>
 */

public final class AdobeUserID
{
  private final String value;

  /**
   * Wrap a user ID.
   *
   * @param in_value The raw user ID.
   */

  public AdobeUserID(
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

    final AdobeUserID that = (AdobeUserID) o;
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
