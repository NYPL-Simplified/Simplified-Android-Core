package org.nypl.simplified.tests.opds2

import org.nypl.simplified.opds2.irradia.OPDS2ParsersIrradia
import org.nypl.simplified.opds2.parser.api.OPDS2ParsersType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OPDS2ParserIrradiaTest : OPDS2ParserContract() {

  override val logger: Logger =
    LoggerFactory.getLogger(OPDS2ParserIrradiaTest::class.java)

  override fun createParsers(): OPDS2ParsersType {
    return OPDS2ParsersIrradia
  }
}
