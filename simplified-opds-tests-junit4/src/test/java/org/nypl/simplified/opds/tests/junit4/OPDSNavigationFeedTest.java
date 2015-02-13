package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSNavigationFeedContract;
import org.nypl.simplified.opds.tests.contracts.OPDSNavigationFeedContractType;

public final class OPDSNavigationFeedTest implements
  OPDSNavigationFeedContractType
{
  private final OPDSNavigationFeedContract contract;

  public OPDSNavigationFeedTest()
  {
    this.contract = new OPDSNavigationFeedContract();
  }

  @Override @Test public void testSerialization()
    throws Exception
  {
    this.contract.testSerialization();
  }
}
