package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.joda.time.DateTime;

import java.net.URI;

/**
 * The book is public domain.
 */

public final class OPDSAvailabilityOpenAccess implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<URI> revoke;

  private OPDSAvailabilityOpenAccess(final OptionType<URI> in_revoke)
  {
    this.revoke = NullCheck.notNull(in_revoke);
  }

  /**
   * Get availability end date (always none for OpenAccess)
   * @return end_date
   */
  public OptionType<DateTime> getEndDate()
  {
    return Option.none();
  }

  /**
   * @param revoke The revocation link, if any
   *
   * @return An "open access" availability value
   */

  public static OPDSAvailabilityOpenAccess get(
    final OptionType<URI> revoke)
  {
    return new OPDSAvailabilityOpenAccess(revoke);
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("OPDSAvailabilityOpenAccess{");
    sb.append("revoke=").append(this.revoke);
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

    final OPDSAvailabilityOpenAccess that = (OPDSAvailabilityOpenAccess) o;
    return this.revoke.equals(that.revoke);
  }

  @Override public int hashCode()
  {
    return this.revoke.hashCode();
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onOpenAccess(this);
  }

  /**
   * @return The revocation link, if any
   */

  public OptionType<URI> getRevoke()
  {
    return this.revoke;
  }
}
