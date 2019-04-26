package org.nypl.simplified.tests.android.books

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@SmallTest
class MIMETypeTest : MIMETypeContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(MIMETypeTest::class.java)

}
