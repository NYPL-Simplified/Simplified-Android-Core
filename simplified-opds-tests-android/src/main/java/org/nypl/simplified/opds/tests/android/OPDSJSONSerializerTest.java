package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSJSONSerializerContract;
import org.nypl.simplified.opds.tests.contracts.OPDSJSONSerializerContractType;

import android.test.InstrumentationTestCase;

public final class OPDSJSONSerializerTest extends InstrumentationTestCase implements
  OPDSJSONSerializerContractType
{
  private final OPDSJSONSerializerContract contract;

  public OPDSJSONSerializerTest()
  {
    this.contract = new OPDSJSONSerializerContract();
  }

  @Override public void testRoundTrip0()
    throws Exception
  {
    this.contract.testRoundTrip0();
  }

  @Override public void testRoundTrip1()
    throws Exception
  {
    this.contract.testRoundTrip1();
  }
}
