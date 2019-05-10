package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.net.URI;

/**
 * The book is held and is ready to be checked out now.
 */

public final class OPDSAvailabilityHeldReady implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<DateTime> end_date;
  private final OptionType<URI>      revoke;

  private OPDSAvailabilityHeldReady(
    final OptionType<DateTime> in_end_date,
    final OptionType<URI> in_revoke)
  {
    this.end_date = NullCheck.notNull(in_end_date);
    this.revoke = NullCheck.notNull(in_revoke);
  }

  /**
   * @param in_end_date The end date (if known)
   * @param in_revoke   The reservation revocation link, if any
   *
   * @return A value that states that a book is on hold
   */

  public static OPDSAvailabilityHeldReady get(
    final OptionType<DateTime> in_end_date,
    final OptionType<URI> in_revoke)
  {
    return new OPDSAvailabilityHeldReady(in_end_date, in_revoke);
  }

  /**
   * @return A URI for revoking the reservation, if any
   */

  public OptionType<URI> getRevoke()
  {
    return this.revoke;
  }

  /**
   * @return The date that the hold will become unavailable
   */

  public OptionType<DateTime> getEndDate()
  {
    return this.end_date;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final OPDSAvailabilityHeldReady that = (OPDSAvailabilityHeldReady) o;
    return this.end_date.equals(that.end_date)
           && this.revoke.equals(that.revoke);
  }

  @Override public int hashCode()
  {
    return this.end_date.hashCode();
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onHeldReady(this);
  }

  @Override public String toString()
  {
    final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAvailabilityHeldReady end_date=");
    this.end_date.map(e -> {
        b.append(fmt.print(e));
        return Unit.unit();
      });
    b.append(" revoke=");
    b.append(this.revoke);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
