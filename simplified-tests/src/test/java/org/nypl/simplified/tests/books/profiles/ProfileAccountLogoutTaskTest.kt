package org.nypl.simplified.tests.books.profiles

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProfileAccountLogoutTaskTest : ProfileAccountLogoutTaskContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(ProfileAccountLogoutTaskTest::class.java)
}
