package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedParserContract;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedParserContractType;

public final class OPDSFeedParserTest implements OPDSFeedParserContractType
{
  private final OPDSFeedParserContractType contract;

  public OPDSFeedParserTest()
  {
    this.contract = new OPDSFeedParserContract();
  }

  @Override @Test public void testNavigationFeed0()
    throws Exception
  {
    this.contract.testNavigationFeed0();
  }

  @Override @Test public
    void
    testNavigationFeedBadEntryFeaturedLinkWithoutHref()
      throws Exception
  {
    this.contract.testNavigationFeedBadEntryFeaturedLinkWithoutHref();
  }

  @Override @Test public void testNavigationFeedBadEntryLinkWithoutHref()
    throws Exception
  {
    this.contract.testNavigationFeedBadEntryLinkWithoutHref();
  }

  @Override @Test public void testNavigationFeedBadEntryNoLinks()
    throws Exception
  {
    this.contract.testNavigationFeedBadEntryNoLinks();
  }

  @Override @Test public
    void
    testNavigationFeedBadEntrySubsectionLinkWithoutHref()
      throws Exception
  {
    this.contract.testNavigationFeedBadEntrySubsectionLinkWithoutHref();
  }
}
