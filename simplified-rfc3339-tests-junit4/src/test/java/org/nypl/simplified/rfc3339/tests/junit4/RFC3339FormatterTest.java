package org.nypl.simplified.rfc3339.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.rfc3339.tests.RFC3339FormatterContract;
import org.nypl.simplified.rfc3339.tests.RFC3339FormatterContractType;

/**
 * JUnit4 test case frontend.
 */

public final class RFC3339FormatterTest implements RFC3339FormatterContractType
{
  private final RFC3339FormatterContract contract;

  public RFC3339FormatterTest()
  {
    this.contract = new RFC3339FormatterContract();
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
