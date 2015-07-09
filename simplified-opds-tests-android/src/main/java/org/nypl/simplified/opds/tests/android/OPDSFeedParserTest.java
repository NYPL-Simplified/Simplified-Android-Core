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

  @Override public void testAcquisitionFeedAvailability()
    throws Exception
  {
    this.contract.testAcquisitionFeedAvailability();
  }

  @Override public void testAcquisitionFeedCategories0()
    throws Exception
  {
    this.contract.testAcquisitionFeedCategories0();
  }

  @Override public void testAcquisitionFeedFacets0()
    throws Exception
  {
    this.contract.testAcquisitionFeedFacets0();
  }

  @Override public void testAcquisitionFeedFiction0()
    throws Exception
  {
    this.contract.testAcquisitionFeedFiction0();
  }

  @Override public void testAcquisitionFeedGroups0()
    throws Exception
  {
    this.contract.testAcquisitionFeedGroups0();
  }

  @Override public void testAcquisitionFeedPaginated0()
    throws Exception
  {
    this.contract.testAcquisitionFeedPaginated0();
  }

  @Override public void testDOMException()
    throws Exception
  {
    this.contract.testDOMException();
  }

  @Override public void testEmpty0()
    throws Exception
  {
    this.contract.testEmpty0();
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
