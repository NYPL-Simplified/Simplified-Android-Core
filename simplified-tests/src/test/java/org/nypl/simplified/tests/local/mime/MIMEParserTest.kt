package org.nypl.simplified.tests.local.mime

import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.mime.MIMEParserType
import org.nypl.simplified.tests.mime.MIMEParserContract

class MIMEParserTest: MIMEParserContract() {

  override fun parser(text: String): MIMEParserType {
    return MIMEParser.create(text)
  }

}
