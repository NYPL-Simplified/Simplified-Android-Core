package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The book is loaned out to the user.
 */

public final class OPDSAvailabilityLoaned implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<Calendar> end_date;
  private final Calendar             start_date;

  private OPDSAvailabilityLoaned(
    final Calendar in_start_date,
    final OptionType<Calendar> in_end_date)
  {
    this.start_date = NullCheck.notNull(in_start_date);
    this.end_date = NullCheck.notNull(in_end_date);
  }

  /**
   * @param in_start_date The start date for the loan
   * @param in_end_date   The end date for the loan
   *
   * @return An availability value that states that the given book is loaned
   */

  public static OPDSAvailabilityLoaned get(
    final Calendar in_start_date,
    final OptionType<Calendar> in_end_date)
  {
    return new OPDSAvailabilityLoaned(in_start_date, in_end_date);
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
           && this.start_date.equals(other.start_date);
  }

  /**
   * @return The end date for the loan, if any
   */

  public OptionType<Calendar> getEndDate()
  {
    return this.end_date;
  }

  /**
   * @return The start date for the loan
   */

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
    return m.onLoaned(this);
  }

  @Override public String toString()
  {
    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAvailabilityLoaned end_date=");
    b.append(
      this.end_date.map(
        new FunctionType<Calendar, String>()
        {
          @Override public String call(
            final Calendar c)
          {
            return NullCheck.notNull(fmt.format(c));
          }
        }));
    b.append(" start_date=");
    b.append(fmt.format(this.start_date.getTime()));
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
