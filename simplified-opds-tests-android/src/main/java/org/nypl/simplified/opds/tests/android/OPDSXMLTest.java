package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSXMLContract;
import org.nypl.simplified.opds.tests.contracts.OPDSXMLContractType;

import android.test.InstrumentationTestCase;

public final class OPDSXMLTest extends InstrumentationTestCase implements
  OPDSXMLContractType
{
  private final OPDSXMLContractType contract;

  public OPDSXMLTest()
  {
    this.contract = new OPDSXMLContract();
  }

  @Override public void testNamespaces_0()
    throws Exception
  {
    this.contract.testNamespaces_0();
  }
}
