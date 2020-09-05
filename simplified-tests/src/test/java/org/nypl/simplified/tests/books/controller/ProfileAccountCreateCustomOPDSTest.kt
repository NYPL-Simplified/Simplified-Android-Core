package org.nypl.simplified.tests.books.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProfileAccountCreateCustomOPDSTest : ProfileAccountCreateCustomOPDSContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(ProfileAccountCreateCustomOPDSTest::class.java)
}
