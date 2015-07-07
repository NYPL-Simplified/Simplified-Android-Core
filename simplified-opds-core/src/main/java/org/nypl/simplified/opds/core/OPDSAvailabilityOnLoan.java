package org.nypl.simplified.opds.core;

import java.util.Calendar;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The book is loaned out to the user.
 */

public final class OPDSAvailabilityOnLoan implements OPDSAvailabilityType
{
  public static OPDSAvailabilityOnLoan get(
    final Calendar in_start_date,
    final OptionType<Calendar> in_end_date)
  {
    return new OPDSAvailabilityOnLoan(in_start_date, in_end_date);
  }

  private final OptionType<Calendar> end_date;
  private final Calendar             start_date;

  private OPDSAvailabilityOnLoan(
    final Calendar in_start_date,
    final OptionType<Calendar> in_end_date)
  {
    this.start_date = NullCheck.notNull(in_start_date);
    this.end_date = NullCheck.notNull(in_end_date);
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
    final OPDSAvailabilityOnLoan other = (OPDSAvailabilityOnLoan) obj;
    return this.end_date.equals(other.end_date)
      && this.start_date.equals(other.start_date);
  }

  public OptionType<Calendar> getEndDate()
  {
    return this.end_date;
  }

  public Calendar getStartDate()
  {
    return this.start_date;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.end_date.hashCode();
    result = (prime * result) + this.start_date.hashCode();
    return result;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onLoan(this);
  }
}
