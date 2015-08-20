package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The book is reserved.
 */

public final class OPDSAvailabilityReserved implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<Calendar> end_date;

  private OPDSAvailabilityReserved(
    final OptionType<Calendar> in_end_date)
  {
    this.end_date = NullCheck.notNull(in_end_date);
  }

  /**
   * @param in_end_date The end date (if known)
   *
   * @return A value that states that a book is on hold
   */

  public static OPDSAvailabilityReserved get(
    final OptionType<Calendar> in_end_date)
  {
    return new OPDSAvailabilityReserved(in_end_date);
  }

  /**
   * @return The date that the hold will become unavailable
   */

  public OptionType<Calendar> getEndDate()
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

    final OPDSAvailabilityReserved that = (OPDSAvailabilityReserved) o;
    return this.end_date.equals(that.end_date);
  }

  @Override public int hashCode()
  {
    return this.end_date.hashCode();
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onReserved(this);
  }

  @Override public String toString()
  {
    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAvailabilityReserved end_date=");
    this.end_date.map(
      new FunctionType<Calendar, Unit>()
      {
        @Override public Unit call(final Calendar e)
        {
          b.append(fmt.format(e.getTime()));
          return Unit.unit();
        }
      });
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
