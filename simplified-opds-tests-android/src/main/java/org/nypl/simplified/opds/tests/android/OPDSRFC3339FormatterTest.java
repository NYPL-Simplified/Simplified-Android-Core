package org.nypl.simplified.opds.tests.android;

import org.nypl.simplified.opds.tests.contracts.OPDSRFC3339FormatterContract;
import org.nypl.simplified.opds.tests.contracts.OPDSRFC3339FormatterContractType;

import android.test.InstrumentationTestCase;

public final class OPDSRFC3339FormatterTest extends InstrumentationTestCase implements
  OPDSRFC3339FormatterContractType
{
  private final OPDSRFC3339FormatterContract contract;

  public OPDSRFC3339FormatterTest()
  {
    this.contract = new OPDSRFC3339FormatterContract();
  }

  @Override public void testDate0()
    throws Exception
  {
    this.contract.testDate0();
  }

  @Override public void testDate1()
    throws Exception
  {
    this.contract.testDate1();
  }

  @Override public void testDate2()
    throws Exception
  {
    this.contract.testDate2();
  }

  @Override public void testDate3()
    throws Exception
  {
    this.contract.testDate3();
  }

  @Override public void testNull()
    throws Exception
  {
    this.contract.testNull();
  }
}
