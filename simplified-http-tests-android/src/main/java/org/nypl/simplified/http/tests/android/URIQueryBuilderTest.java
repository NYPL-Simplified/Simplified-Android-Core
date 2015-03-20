package org.nypl.simplified.http.tests.android;

import org.nypl.simplified.http.tests.contracts.URIQueryBuilderContract;
import org.nypl.simplified.http.tests.contracts.URIQueryBuilderContractType;

import android.test.InstrumentationTestCase;

public final class URIQueryBuilderTest extends InstrumentationTestCase implements
  URIQueryBuilderContractType
{
  private final URIQueryBuilderContract contract;

  public URIQueryBuilderTest()
  {
    this.contract = new URIQueryBuilderContract();
  }

  @Override public void testQueryEncode_0()
  {
    this.contract.testQueryEncode_0();
  }

  @Override public void testQueryEncode_1()
    throws Exception
  {
    this.contract.testQueryEncode_1();
  }

  @Override public void testQueryEncode_2()
    throws Exception
  {
    this.contract.testQueryEncode_2();
  }
}
