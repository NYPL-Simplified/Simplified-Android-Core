package org.nypl.simplified.mime

interface MIMEParserType {

  fun parse(): MIMEParserResult

  @Throws(Exception::class)
  fun parseExceptionally(): MIMEType {
    val result = this.parse()
    return when (result) {
      is MIMEParserResult.Success -> result.type
      is MIMEParserResult.Failure -> throw result.exception
    }
  }
}
