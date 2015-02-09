package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSFeedLoaderContract;
import org.nypl.simplified.opds.tests.contracts.OPDSFeedLoaderContractType;

import android.test.InstrumentationTestCase;

public final class OPDSFeedLoaderTest extends InstrumentationTestCase implements
  OPDSFeedLoaderContractType
{
  private final OPDSFeedLoaderContract contract;

  public OPDSFeedLoaderTest()
  {
    this.contract = new OPDSFeedLoaderContract();
  }

  @Override public void testLoaderErrorCorrect()
    throws Exception
  {
    this.contract.testLoaderErrorCorrect();
  }

  @Override public void testLoaderSuccessCorrect()
    throws Exception
  {
    this.contract.testLoaderSuccessCorrect();
  }
}
