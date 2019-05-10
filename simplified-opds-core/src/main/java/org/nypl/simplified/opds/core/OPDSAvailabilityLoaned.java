package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.net.URI;

/**
 * The book is loaned out to the user.
 */

public final class OPDSAvailabilityLoaned implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<DateTime> end_date;
  private final OptionType<DateTime> start_date;
  private final OptionType<URI>      revoke;

  private OPDSAvailabilityLoaned(
    final OptionType<DateTime> in_start_date,
    final OptionType<DateTime> in_end_date,
    final OptionType<URI> in_revoke)
  {
    this.start_date = NullCheck.notNull(in_start_date);
    this.end_date = NullCheck.notNull(in_end_date);
    this.revoke = NullCheck.notNull(in_revoke);
  }

  /**
   * @param in_start_date The start date for the loan
   * @param in_end_date   The end date for the loan
   * @param in_revoke     The optional revocation link for the loan
   *
   * @return An availability value that states that the given book is loaned
   */

  public static OPDSAvailabilityLoaned get(
    final OptionType<DateTime> in_start_date,
    final OptionType<DateTime> in_end_date,
    final OptionType<URI> in_revoke)
  {
    return new OPDSAvailabilityLoaned(in_start_date, in_end_date, in_revoke);
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
    final OPDSAvailabilityLoaned other = (OPDSAvailabilityLoaned) obj;
    return this.end_date.equals(other.end_date)
           && this.start_date.equals(other.start_date)
           && this.revoke.equals(other.revoke);
  }

  /**
   * @return The end date for the loan, if any
   */

  public OptionType<DateTime> getEndDate()
  {
    return this.end_date;
  }

  /**
   * @return A URI for revoking the hold, if any
   */

  public OptionType<URI> getRevoke()
  {
    return this.revoke;
  }

  /**
   * @return The start date for the loan, if any
   */

  public OptionType<DateTime> getStartDate()
  {
    return this.start_date;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.end_date.hashCode();
    result = (prime * result) + this.start_date.hashCode();
    result = (prime * result) + this.revoke.hashCode();
    return result;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onLoaned(this);
  }

  @Override public String toString()
  {
    final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAvailabilityLoaned end_date=");
    b.append(this.end_date.map(c -> NullCheck.notNull(fmt.print(c))));
    b.append(" start_date=");
    b.append(this.start_date.map(c -> NullCheck.notNull(fmt.print(c))));
    b.append(" revoke=");
    b.append(this.revoke);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
