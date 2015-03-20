package org.nypl.simplified.http.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.http.tests.contracts.URIQueryBuilderContract;
import org.nypl.simplified.http.tests.contracts.URIQueryBuilderContractType;

public final class URIQueryBuilderTest implements URIQueryBuilderContractType
{
  private final URIQueryBuilderContract contract;

  public URIQueryBuilderTest()
  {
    this.contract = new URIQueryBuilderContract();
  }

  @Override @Test public void testQueryEncode_0()
  {
    this.contract.testQueryEncode_0();
  }

  @Override @Test public void testQueryEncode_1()
    throws Exception
  {
    this.contract.testQueryEncode_1();
  }

  @Override @Test public void testQueryEncode_2()
    throws Exception
  {
    this.contract.testQueryEncode_2();
  }
}
