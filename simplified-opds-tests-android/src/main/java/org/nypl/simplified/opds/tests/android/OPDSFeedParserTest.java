package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSFeedParserContract;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedParserContractType;

import android.test.InstrumentationTestCase;

public final class OPDSFeedParserTest extends InstrumentationTestCase implements
  OPDSFeedParserContractType
{
  private final OPDSFeedParserContractType contract;

  public OPDSFeedParserTest()
  {
    this.contract = new OPDSFeedParserContract();
  }

  @Override public void testAcquisitionFeedFiction0()
    throws Exception
  {
    this.contract.testAcquisitionFeedFiction0();
  }

  @Override public void testDOMException()
    throws Exception
  {
    this.contract.testDOMException();
  }

  @Override public void testNavigationFeed0()
    throws Exception
  {
    this.contract.testNavigationFeed0();
  }

  @Override public void testNavigationFeedBadEntryFeaturedLinkWithoutHref()
    throws Exception
  {
    this.contract.testNavigationFeedBadEntryFeaturedLinkWithoutHref();
  }

  @Override public void testNavigationFeedBadEntryLinkWithoutHref()
    throws Exception
  {
    this.contract.testNavigationFeedBadEntryLinkWithoutHref();
  }

  @Override public void testNavigationFeedBadEntryNoLinks()
    throws Exception
  {
    this.contract.testNavigationFeedBadEntryNoLinks();
  }

  @Override public void testNavigationFeedBadEntrySubsectionLinkWithoutHref()
    throws Exception
  {
    this.contract.testNavigationFeedBadEntrySubsectionLinkWithoutHref();
  }

  @Override public void testNotXMLException()
    throws Exception
  {
    this.contract.testNotXMLException();
  }

  @Override public void testParserURISyntaxException()
    throws Exception
  {
    this.contract.testParserURISyntaxException();
  }

  @Override public void testStreamIOException()
    throws Exception
  {
    this.contract.testStreamIOException();
  }
}
