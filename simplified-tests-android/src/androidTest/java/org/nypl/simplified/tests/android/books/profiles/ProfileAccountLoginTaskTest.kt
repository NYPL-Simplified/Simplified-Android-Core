package org.nypl.simplified.tests.android.books.profiles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
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
