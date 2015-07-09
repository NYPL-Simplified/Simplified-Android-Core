package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSJSONSerializerContract;
import org.nypl.simplified.opds.tests.contracts.OPDSJSONSerializerContractType;

public final class OPDSJSONSerializerTest implements
  OPDSJSONSerializerContractType
{
  private final OPDSJSONSerializerContract contract;

  public OPDSJSONSerializerTest()
  {
    this.contract = new OPDSJSONSerializerContract();
  }

  @Override @Test public void testRoundTrip0()
    throws Exception
  {
    this.contract.testRoundTrip0();
  }

  @Override @Test public void testRoundTrip1()
    throws Exception
  {
    this.contract.testRoundTrip1();
  }
}
