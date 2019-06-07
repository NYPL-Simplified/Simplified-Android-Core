package org.nypl.simplified.tests.android.books.profiles

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.simplified.tests.books.profiles.ProfileAccountLogoutTaskContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProfileAccountLogoutTaskTest : ProfileAccountLogoutTaskContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(ProfileAccountLogoutTaskTest::class.java)

}
