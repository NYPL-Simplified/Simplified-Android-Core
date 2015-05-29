package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedEntrySerializerContract;
import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedEntrySerializerContractType;

import android.test.InstrumentationTestCase;

public final class OPDSAcquisitionFeedEntrySerializerTest extends
  InstrumentationTestCase implements
  OPDSAcquisitionFeedEntrySerializerContractType
{
  private final OPDSAcquisitionFeedEntrySerializerContract contract;

  public OPDSAcquisitionFeedEntrySerializerTest()
  {
    this.contract = new OPDSAcquisitionFeedEntrySerializerContract();
  }

  @Override public void testRoundTrip0()
    throws Exception
  {
    this.contract.testRoundTrip0();
  }
}
