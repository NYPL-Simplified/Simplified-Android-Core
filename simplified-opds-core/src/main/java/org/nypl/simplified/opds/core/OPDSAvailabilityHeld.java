package org.nypl.simplified.opds.core;

import java.util.Calendar;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The book is on hold.
 */

public final class OPDSAvailabilityHeld implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;

  public static OPDSAvailabilityHeld get(
    final Calendar in_start_date,
    final int in_position)
  {
    return new OPDSAvailabilityHeld(in_start_date, in_position);
  }

  private final int      position;
  private final Calendar start_date;

  private OPDSAvailabilityHeld(
    final Calendar in_start_date,
    final int in_position)
  {
    this.start_date = NullCheck.notNull(in_start_date);
    this.position = in_position;
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
    final OPDSAvailabilityHeld other = (OPDSAvailabilityHeld) obj;
    return (this.position == other.position)
      && this.start_date.equals(other.start_date);
  }

  public int getPosition()
  {
    return this.position;
  }

  public Calendar getStartDate()
  {
    return this.start_date;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.position;
    result = (prime * result) + this.start_date.hashCode();
    return result;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onHeld(this);
  }
}
