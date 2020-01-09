package org.nypl.simplified.tests.local.books.profiles

import org.nypl.simplified.tests.books.profiles.ProfileAccountLogoutTaskContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProfileAccountLogoutTaskTest : ProfileAccountLogoutTaskContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(ProfileAccountLogoutTaskTest::class.java)
}
