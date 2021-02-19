package org.nypl.drm.core;

import java.util.Objects;

import java.nio.ByteBuffer;

/**
 * The rights associated with a fulfilled book.
 */

public final class AdobeAdeptLoan
{
  private final ByteBuffer  serialized;
  private final boolean     returnable;
  private final AdobeLoanID loan;

  /**
   * Construct a set of rights.
   *
   * @param in_loan       The loan ID
   * @param in_serialized The serialized form of the rights
   * @param in_returnable {@code true} iff the loan is returnable
   */

  public AdobeAdeptLoan(
    final AdobeLoanID in_loan,
    final ByteBuffer in_serialized,
    final boolean in_returnable)
  {
    this.loan = Objects.requireNonNull(in_loan);
    this.serialized = Objects.requireNonNull(in_serialized);
    this.returnable = in_returnable;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final AdobeAdeptLoan that = (AdobeAdeptLoan) o;
    if (this.isReturnable() != that.isReturnable()) {
      return false;
    }
    if (!this.getSerialized().equals(that.getSerialized())) {
      return false;
    }
    return this.getID().equals(that.getID());
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("AdobeAdeptLoan{");
    sb.append("loan=").append(this.loan);
    sb.append(", serialized=").append(this.serialized);
    sb.append(", returnable=").append(this.returnable);
    sb.append('}');
    return sb.toString();
  }

  @Override public int hashCode()
  {
    int result = this.getSerialized().hashCode();
    result = 31 * result + (this.isReturnable() ? 1 : 0);
    result = 31 * result + this.getID().hashCode();
    return result;
  }

  /**
   * @return The loan ID
   */

  public AdobeLoanID getID()
  {
    return this.loan;
  }

  /**
   * @return {@code true} iff the loan is returnable
   */

  public boolean isReturnable()
  {
    return this.returnable;
  }

  /**
   * @return The serialized form of the rights
   */

  public ByteBuffer getSerialized()
  {
    return this.serialized;
  }
}
