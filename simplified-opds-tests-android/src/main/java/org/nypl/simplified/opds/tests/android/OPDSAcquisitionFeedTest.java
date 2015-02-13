package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedContract;
import org.nypl.simplified.opds.tests.contracts.OPDSAcquisitionFeedContractType;

import android.test.InstrumentationTestCase;

public final class OPDSAcquisitionFeedTest extends InstrumentationTestCase implements
  OPDSAcquisitionFeedContractType
{
  private final OPDSAcquisitionFeedContract contract;

  public OPDSAcquisitionFeedTest()
  {
    this.contract = new OPDSAcquisitionFeedContract();
  }

  @Override public void testSerialization()
    throws Exception
  {
    this.contract.testSerialization();
  }
}
