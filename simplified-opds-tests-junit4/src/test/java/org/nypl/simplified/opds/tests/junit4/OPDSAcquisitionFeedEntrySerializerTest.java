package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedEntrySerializerContract;
import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedEntrySerializerContractType;

public final class OPDSAcquisitionFeedEntrySerializerTest implements
  OPDSAcquisitionFeedEntrySerializerContractType
{
  private final OPDSAcquisitionFeedEntrySerializerContract contract;

  public OPDSAcquisitionFeedEntrySerializerTest()
  {
    this.contract = new OPDSAcquisitionFeedEntrySerializerContract();
  }

  @Override @Test public void testRoundTrip0()
    throws Exception
  {
    this.contract.testRoundTrip0();
  }
}
