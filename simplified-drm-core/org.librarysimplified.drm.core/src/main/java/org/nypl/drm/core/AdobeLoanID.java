package org.nypl.drm.core;

import java.util.Objects;

/**
 * <p>An Adobe loan ID, typically in {@code urn:} form. This is an opaque value
 * that uniquely identifies a loan, and is given to the user during
 * fulfillment.</p>
 *
 * <p>This type exists to reduce the "everything is a string" aspect of the
 * native code API, and therefore to reduce any possibility of accidentally
 * mixing up values when calling functions.</p>
 */

public final class AdobeLoanID
{
  private final String value;

  /**
   * Wrap a loan ID.
   *
   * @param in_value The raw loan ID.
   */

  public AdobeLoanID(
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

    final AdobeLoanID that = (AdobeLoanID) o;
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
