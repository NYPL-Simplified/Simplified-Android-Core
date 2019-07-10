package org.nypl.simplified.opds.auth_document

import com.fasterxml.jackson.databind.ObjectMapper
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParserType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import java.io.InputStream
import java.net.URI

class AuthenticationDocumentParsers : AuthenticationDocumentParsersType {

  private val mapper = ObjectMapper()

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): AuthenticationDocumentParserType =
    AuthenticationDocumentParser(this.mapper, uri, stream, warningsAsErrors)

}