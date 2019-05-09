package org.nypl.simplified.tests.android.mime

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.simplified.mime.MIMELexer
import org.nypl.simplified.mime.MIMELexerType
import org.nypl.simplified.tests.mime.MIMELexerContract

@RunWith(AndroidJUnit4::class)
@SmallTest
class MIMELexerTest : MIMELexerContract() {

  override fun lexer(text: String): MIMELexerType {
    return MIMELexer.create(text)
  }

}