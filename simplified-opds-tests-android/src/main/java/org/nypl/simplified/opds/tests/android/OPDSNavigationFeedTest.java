package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSNavigationFeedContract;
import org.nypl.simplified.opds.tests.contracts.OPDSNavigationFeedContractType;

import android.test.InstrumentationTestCase;

public final class OPDSNavigationFeedTest extends InstrumentationTestCase implements
  OPDSNavigationFeedContractType
{
  private final OPDSNavigationFeedContract contract;

  public OPDSNavigationFeedTest()
  {
    this.contract = new OPDSNavigationFeedContract();
  }

  @Override public void testSerialization()
    throws Exception
  {
    this.contract.testSerialization();
  }
}
