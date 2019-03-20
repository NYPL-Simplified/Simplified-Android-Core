package org.nypl.simplified.tests.local.mime

import org.nypl.simplified.mime.MIMELexer
import org.nypl.simplified.mime.MIMELexerType
import org.nypl.simplified.tests.mime.MIMELexerContract

class MIMELexerTest : MIMELexerContract() {

  override fun lexer(text: String): MIMELexerType {
    return MIMELexer.create(text)
  }

}