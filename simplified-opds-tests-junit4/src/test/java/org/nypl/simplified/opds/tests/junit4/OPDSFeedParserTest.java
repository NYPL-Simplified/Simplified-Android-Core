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

  @Override @Test public void testAcquisitionFeedFiction0()
    throws Exception
  {
    this.contract.testAcquisitionFeedFiction0();
  }

  @Override @Test public void testAcquisitionFeedPaginated0()
    throws Exception
  {
    this.contract.testAcquisitionFeedPaginated0();
  }

  @Override @Test public void testDOMException()
    throws Exception
  {
    this.contract.testDOMException();
  }

  @Override @Test public void testEmpty0()
    throws Exception
  {
    this.contract.testEmpty0();
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

  @Override @Test public void testNotXMLException()
    throws Exception
  {
    this.contract.testNotXMLException();
  }

  @Override @Test public void testParserURISyntaxException()
    throws Exception
  {
    this.contract.testParserURISyntaxException();
  }

  @Override @Test public void testStreamIOException()
    throws Exception
  {
    this.contract.testStreamIOException();
  }

  @Override @Test public void testAcquisitionFeedBlocks0()
    throws Exception
  {
    this.contract.testAcquisitionFeedBlocks0();
  }
}
