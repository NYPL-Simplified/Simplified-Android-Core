package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts.OPDSRFC3339FormatterContract;
import org.nypl.simplified.opds.tests.contracts.OPDSRFC3339FormatterContractType;

public final class OPDSRFC3339FormatterTest implements
  OPDSRFC3339FormatterContractType
{
  private final OPDSRFC3339FormatterContract contract;

  public OPDSRFC3339FormatterTest()
  {
    this.contract = new OPDSRFC3339FormatterContract();
  }

  @Override @Test public void testDate0()
    throws Exception
  {
    this.contract.testDate0();
  }

  @Override @Test public void testDate1()
    throws Exception
  {
    this.contract.testDate1();
  }

  @Override @Test public void testDate2()
    throws Exception
  {
    this.contract.testDate2();
  }

  @Override @Test public void testDate3()
    throws Exception
  {
    this.contract.testDate3();
  }

  @Override @Test public void testNull()
    throws Exception
  {
    this.contract.testNull();
  }
}
