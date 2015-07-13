package org.nypl.simplified.opds.core;

/**
 * The book is not available for borrowing but is available to place on hold.
 */

public final class OPDSAvailabilityHoldable implements OPDSAvailabilityType
{
  private static final OPDSAvailabilityHoldable INSTANCE;
  private static final long                     serialVersionUID = 1L;

  static {
    INSTANCE = new OPDSAvailabilityHoldable();
  }

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
