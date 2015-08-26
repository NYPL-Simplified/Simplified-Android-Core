package org.nypl.simplified.opds.tests.junit4;

import org.junit.Test;
import org.nypl.simplified.opds.tests.contracts
  .OPDSAuthenticationDocumentParserContract;
import org.nypl.simplified.opds.tests.contracts
  .OPDSAuthenticationDocumentParserContractType;

/**
 * JUnit 4 frontend to test contract.
 */

public final class OPDSAuthenticationDocumentParserTest
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

  @Override @Test public void testParseSpecific_0()
    throws Exception
  {
    this.contract.testParseSpecific_0();
  }
}
