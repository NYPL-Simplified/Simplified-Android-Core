package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedEntryParserContract;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedEntryParserContractType;

public final class OPDSFeedEntryParserTest
  implements OPDSFeedEntryParserContractType
{
  private final OPDSFeedEntryParserContractType contract;

  public OPDSFeedEntryParserTest()
  {
    this.contract = new OPDSFeedEntryParserContract();
  }

  @Override @Test public void testEntryAvailabilityHoldable()
    throws Exception
  {
    this.contract.testEntryAvailabilityHoldable();
  }

  @Override @Test public void testEntryAvailabilityLoanedTimed()
    throws Exception
  {
    this.contract.testEntryAvailabilityLoanedTimed();
  }

  @Override @Test public void testEntryAvailabilityOpenAccess()
    throws Exception
  {
    this.contract.testEntryAvailabilityOpenAccess();
  }

  @Override @Test public void testEntryAvailabilityLoanedIndefinite()
    throws Exception
  {
    this.contract.testEntryAvailabilityLoanedIndefinite();
  }

  @Override @Test public void testEntryAvailabilityLoanable()
    throws Exception
  {
    this.contract.testEntryAvailabilityLoanable();
  }

  @Override @Test public void testEntryAvailabilityHeldIndefinite()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldIndefinite();
  }

  @Override @Test public void testEntryAvailabilityHeldIndefiniteQueued()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldIndefiniteQueued();
  }

  @Override @Test public void testEntryAvailabilityHeldTimed()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldTimed();
  }

  @Override @Test public void testEntryAvailabilityHeldTimedQueued()
    throws Exception
  {
    this.contract.testEntryAvailabilityHeldTimedQueued();
  }
}
