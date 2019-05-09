package org.nypl.simplified.tests.android.mime

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.mime.MIMEParserType
import org.nypl.simplified.tests.mime.MIMEParserContract

@RunWith(AndroidJUnit4::class)
@SmallTest
class MIMEParserTest: MIMEParserContract() {

  override fun parser(text: String): MIMEParserType {
    return MIMEParser.create(text)
  }

}
