package org.nypl.simplified.opds.core;

/**
 * The book is available for borrowing.
 */

public final class OPDSAvailabilityLoanable implements OPDSAvailabilityType
{
  private static final OPDSAvailabilityLoanable INSTANCE;
  private static final long serialVersionUID = 1L;

  static {
    INSTANCE = new OPDSAvailabilityLoanable();
  }

  private OPDSAvailabilityLoanable()
  {

  }

  /**
   * @return An availability value stating that a book is available for loan
   */

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

  @Override public String toString()
  {
    return "[OPDSAvailabilityLoanable]";
  }
}
