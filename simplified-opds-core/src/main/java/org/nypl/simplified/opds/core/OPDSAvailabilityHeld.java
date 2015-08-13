package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import java.util.Calendar;

/**
 * The book is on hold.
 */

public final class OPDSAvailabilityHeld implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final OptionType<Integer> position;
  private final Calendar            start_date;

  private OPDSAvailabilityHeld(
    final Calendar in_start_date,
    final OptionType<Integer> in_position)
  {
    this.start_date = NullCheck.notNull(in_start_date);
    this.position = NullCheck.notNull(in_position);
  }

  /**
   * @param in_start_date The start date
   * @param in_position   The queue position
   *
   * @return A value that states that a book is on hold
   */

  public static OPDSAvailabilityHeld get(
    final Calendar in_start_date,
    final OptionType<Integer> in_position)
  {
    return new OPDSAvailabilityHeld(in_start_date, in_position);
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
    if (!this.position.equals(that.position)) {
      return false;
    }
    return this.start_date.equals(that.start_date);
  }

  @Override public int hashCode()
  {
    int result = this.position.hashCode();
    result = 31 * result + this.start_date.hashCode();
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
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAvailabilityHeld position=");
    b.append(this.position);
    b.append(" start_date=");
    b.append(
      OPDSRFC3339Formatter.newDateFormatter().format(
        this.start_date.getTime()));
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
