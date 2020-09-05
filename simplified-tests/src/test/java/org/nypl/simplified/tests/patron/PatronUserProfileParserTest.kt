package org.nypl.simplified.tests.patron

import org.nypl.simplified.patron.PatronUserProfileParsers
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PatronUserProfileParserTest : PatronUserProfileParserContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(PatronUserProfileParserTest::class.java)

  override val parsers: PatronUserProfileParsersType
    get() = PatronUserProfileParsers()
}
