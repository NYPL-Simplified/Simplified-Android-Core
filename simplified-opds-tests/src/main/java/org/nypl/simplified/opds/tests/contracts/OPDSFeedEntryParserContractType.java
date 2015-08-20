package org.nypl.simplified.opds.tests.contracts;

/**
 * The type of feed entry parser contracts.
 */

public interface OPDSFeedEntryParserContractType
{
  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityLoanable()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityLoanedIndefinite()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityLoanedTimed()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityHoldable()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityHeldIndefinite()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityHeldTimed()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityHeldIndefiniteQueued()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityHeldTimedQueued()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityReserved()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityReservedTimed()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityOpenAccess()
    throws Exception;

  /**
   * Test that a given entry has the expected availability.
   *
   * @throws Exception On errors
   */

  void testEntryAvailabilityReservedSpecific0()
    throws Exception;
}
