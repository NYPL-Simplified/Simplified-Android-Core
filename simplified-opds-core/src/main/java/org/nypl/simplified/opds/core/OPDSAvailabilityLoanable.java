package org.nypl.simplified.opds.core;

/**
 * The book is available for borrowing.
 */

public final class OPDSAvailabilityLoanable implements OPDSAvailabilityType
{
  private static final OPDSAvailabilityLoanable INSTANCE;

  static {
    INSTANCE = new OPDSAvailabilityLoanable();
  }

  public static OPDSAvailabilityLoanable get()
  {
    return OPDSAvailabilityLoanable.INSTANCE;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
      throws E
  {
    return m.onLoanable(this);
  }
}
