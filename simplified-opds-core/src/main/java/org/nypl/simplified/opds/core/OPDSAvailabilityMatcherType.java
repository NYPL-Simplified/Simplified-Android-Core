package org.nypl.simplified.opds.core;

/**
 * <p>
 * The type of book availability matchers.
 * </p>
 */

public interface OPDSAvailabilityMatcherType<A, E extends Exception>
{
  A onHeld(
    OPDSAvailabilityHeld a)
      throws E;

  A onHoldable(
    OPDSAvailabilityHoldable a)
      throws E;

  A onLoaned(
    OPDSAvailabilityLoaned a)
      throws E;

  A onLoanable(
    OPDSAvailabilityLoanable a)
      throws E;

  A onOpenAccess(
    OPDSAvailabilityOpenAccess a)
      throws E;
}
