package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import java.util.Calendar;

/**
 * The book is not available for borrowing but is available to place on hold.
 */

public final class OPDSAvailabilityHoldable implements OPDSAvailabilityType
{
  private static final OPDSAvailabilityHoldable INSTANCE;
  private static final long serialVersionUID = 1L;

  static {
    INSTANCE = new OPDSAvailabilityHoldable();
  }

  private OPDSAvailabilityHoldable()
  {

  }

  /**
   * Get availability end date (always none for Holdable)
   * @return end_date
   */
  public OptionType<Calendar> getEndDate()
  {
    return Option.none();
  }

  /**
   * @return An availability value stating that a book is available for hold
   */

  public static OPDSAvailabilityHoldable get()
  {
    return OPDSAvailabilityHoldable.INSTANCE;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onHoldable(this);
  }

  @Override public String toString()
  {
    return "[OPDSAvailabilityHoldable]";
  }
}
