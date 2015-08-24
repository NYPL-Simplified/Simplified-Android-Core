package org.nypl.simplified.opds.tests.android;

import android.test.InstrumentationTestCase;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedEntryParserContract;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedEntryParserContractType;

/**
 * Android frontend to the feed entry parser test.
 */

public final class OPDSFeedEntryParserTest extends InstrumentationTestCase
  implements OPDSFeedEntryParserContractType
{
  private final OPDSFeedEntryParserContractType contract;

  /**
   * Construct a test.
   */

  public OPDSFeedEntryParserTest()
  {
    this.contract = new OPDSFeedEntryParserContract();
  }

  @Override public void testEntryAvailabilityHeldIndefinite()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldIndefinite();
  }

  @Override public void testEntryAvailabilityHeldIndefiniteQueued()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldIndefiniteQueued();
  }

  @Override public void testEntryAvailabilityHeldTimed()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldTimed();
  }

  @Override public void testEntryAvailabilityHeldTimedQueued()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldTimedQueued();
  }

  @Override public void testEntryAvailabilityHoldable()
    throws Exception
  {
    this.contract.testEntryAvailabilityHoldable();
  }

  @Override public void testEntryAvailabilityLoanable()
    throws Exception
  {
    this.contract.testEntryAvailabilityLoanable();
  }

  @Override public void testEntryAvailabilityLoanedIndefinite()
    throws Exception
  {
    this.contract.testEntryAvailabilityLoanedIndefinite();
  }

  @Override public void testEntryAvailabilityLoanedTimed()
    throws Exception
  {
    this.contract.testEntryAvailabilityLoanedTimed();
  }

  @Override public void testEntryAvailabilityOpenAccess()
    throws Exception
  {
    this.contract.testEntryAvailabilityOpenAccess();
  }

  @Override public void testEntryAvailabilityReserved()
    throws Exception
  {
    this.contract.testEntryAvailabilityReserved();
  }

  @Override public void testEntryAvailabilityReservedSpecific0()
    throws Exception
  {
    this.contract.testEntryAvailabilityReservedSpecific0();
  }

  @Override public void testEntryAvailabilityReservedTimed()
    throws Exception
  {
    this.contract.testEntryAvailabilityReservedTimed();
  }
}
