package org.nypl.simplified.opds.tests.android;

import android.test.InstrumentationTestCase;
import org.nypl.simplified.opds.tests.contracts
  .OPDSAuthenticationDocumentParserContract;
import org.nypl.simplified.opds.tests.contracts
  .OPDSAuthenticationDocumentParserContractType;

/**
 * JUnit 3 frontend to test contract.
 */

public final class OPDSAuthenticationDocumentParserTest
  extends InstrumentationTestCase
  implements OPDSAuthenticationDocumentParserContractType
{
  private final OPDSAuthenticationDocumentParserContractType contract;

  /**
   * Construct the test.
   */

  public OPDSAuthenticationDocumentParserTest()
  {
    this.contract = new OPDSAuthenticationDocumentParserContract();
  }

  @Override public void testParseSpecific_0()
    throws Exception
  {
    this.contract.testParseSpecific_0();
  }
}
