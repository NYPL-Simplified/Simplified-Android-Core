package org.nypl.simplified.tests.android.books.profiles

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.simplified.tests.books.profiles.ProfileAccountLoginTaskContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProfileAccountLoginTaskTest : ProfileAccountLoginTaskContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(ProfileAccountLoginTaskTest::class.java)

}
