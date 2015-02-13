package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedContract;
import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedContractType;

public final class OPDSAcquisitionFeedTest implements
  OPDSAcquisitionFeedContractType
{
  private final OPDSAcquisitionFeedContract contract;

  public OPDSAcquisitionFeedTest()
  {
    this.contract = new OPDSAcquisitionFeedContract();
  }

  @Override @Test public void testSerialization()
    throws Exception
  {
    this.contract.testSerialization();
  }
}
