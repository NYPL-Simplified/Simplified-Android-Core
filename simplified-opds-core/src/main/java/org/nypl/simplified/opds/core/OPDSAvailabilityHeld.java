package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The book is on hold.
 */

public final class OPDSAvailabilityHeld implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<Integer>  position;
  private final Calendar             start_date;
  private final OptionType<Calendar> end_date;

  private OPDSAvailabilityHeld(
    final Calendar in_start_date,
    final OptionType<Integer> in_position,
    final OptionType<Calendar> in_end_date)
  {
    this.start_date = NullCheck.notNull(in_start_date);
    this.position = NullCheck.notNull(in_position);
    this.end_date = NullCheck.notNull(in_end_date);
  }

  /**
   * @param in_start_date The start date
   * @param in_position   The queue position
   * @param in_end_date   The end date (if known)
   *
   * @return A value that states that a book is on hold
   */

  public static OPDSAvailabilityHeld get(
    final Calendar in_start_date,
    final OptionType<Integer> in_position,
    final OptionType<Calendar> in_end_date)
  {
    return new OPDSAvailabilityHeld(in_start_date, in_position, in_end_date);
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

    final OPDSAvailabilityHeld that = (OPDSAvailabilityHeld) o;
    return this.position.equals(that.position)
           && this.start_date.equals(that.start_date)
           && this.end_date.equals(that.end_date);
  }

  @Override public int hashCode()
  {
    int result = this.position.hashCode();
    result = 31 * result + this.start_date.hashCode();
    result = 31 * result + this.position.hashCode();
    result = 31 * result + this.end_date.hashCode();
    return result;
  }

  /**
   * @return The queue position
   */

  public OptionType<Integer> getPosition()
  {
    return this.position;
  }

  /**
   * @return The start date
   */

  public Calendar getStartDate()
  {
    return this.start_date;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onHeld(this);
  }

  @Override public String toString()
  {
    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAvailabilityHeld position=");
    b.append(this.position);
    b.append(" start_date=");
    b.append(fmt.format(this.start_date.getTime()));
    b.append(" end_date=");
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
