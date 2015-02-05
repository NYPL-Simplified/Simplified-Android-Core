package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedLoaderContract;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedLoaderContractType;

public final class OPDSFeedLoaderTest implements OPDSFeedLoaderContractType
{
  private final OPDSFeedLoaderContract contract;

  public OPDSFeedLoaderTest()
  {
    this.contract = new OPDSFeedLoaderContract();
  }

  @Override @Test public void testLoaderErrorCorrect()
    throws Exception
  {
    this.contract.testLoaderErrorCorrect();
  }

  @Override @Test public void testLoaderSuccessCorrect()
    throws Exception
  {
    this.contract.testLoaderSuccessCorrect();
  }
}
