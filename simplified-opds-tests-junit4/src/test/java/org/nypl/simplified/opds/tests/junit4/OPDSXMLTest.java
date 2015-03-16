package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSXMLContract;
import org.nypl.simplified.opds.tests.contracts.OPDSXMLContractType;

public final class OPDSXMLTest implements OPDSXMLContractType
{
  private final OPDSXMLContract contract;

  public OPDSXMLTest()
  {
    this.contract = new OPDSXMLContract();
  }

  @Override @Test public void testNamespaces_0()
    throws Exception
  {
    this.contract.testNamespaces_0();
  }
}
