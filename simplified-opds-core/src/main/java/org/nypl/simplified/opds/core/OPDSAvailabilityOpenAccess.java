package org.nypl.simplified.opds.core;

/**
 * The book is public domain.
 */

public final class OPDSAvailabilityOpenAccess implements OPDSAvailabilityType
{
  private static final long                       serialVersionUID = 1L;
  private static final OPDSAvailabilityOpenAccess INSTANCE;

  static {
    INSTANCE = new OPDSAvailabilityOpenAccess();
  }

  public static OPDSAvailabilityOpenAccess get()
  {
    return OPDSAvailabilityOpenAccess.INSTANCE;
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onOpenAccess(this);
  }

  @Override public String toString()
  {
    return "[OPDSAvailabilityOpenAccess]";
  }
}
