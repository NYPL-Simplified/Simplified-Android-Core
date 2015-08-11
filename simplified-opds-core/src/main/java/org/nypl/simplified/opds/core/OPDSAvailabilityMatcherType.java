package org.nypl.simplified.opds.core;

/**
 * <p> The type of book availability matchers. </p>
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface OPDSAvailabilityMatcherType<A, E extends Exception>
{
  /**
   * Match an availability value.
   *
   * @param a The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onHeld(
    OPDSAvailabilityHeld a)
    throws E;

  /**
   * Match an availability value.
   *
   * @param a The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onHoldable(
    OPDSAvailabilityHoldable a)
    throws E;

  /**
   * Match an availability value.
   *
   * @param a The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onLoaned(
    OPDSAvailabilityLoaned a)
    throws E;

  /**
   * Match an availability value.
   *
   * @param a The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onLoanable(
    OPDSAvailabilityLoanable a)
    throws E;

  /**
   * Match an availability value.
   *
   * @param a The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onOpenAccess(
    OPDSAvailabilityOpenAccess a)
    throws E;
}
