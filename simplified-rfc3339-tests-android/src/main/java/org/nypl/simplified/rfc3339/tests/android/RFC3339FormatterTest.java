package org.nypl.simplified.rfc3339.tests.android;

import android.test.InstrumentationTestCase;
import org.nypl.simplified.rfc3339.tests.RFC3339FormatterContract;
import org.nypl.simplified.rfc3339.tests.RFC3339FormatterContractType;

/**
 * Android unit test frontend.
 */

public final class RFC3339FormatterTest extends InstrumentationTestCase
  implements RFC3339FormatterContractType
{
  private final RFC3339FormatterContract contract;

  /**
   * Construct a test.
   */

  public RFC3339FormatterTest()
  {
    this.contract = new RFC3339FormatterContract();
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
