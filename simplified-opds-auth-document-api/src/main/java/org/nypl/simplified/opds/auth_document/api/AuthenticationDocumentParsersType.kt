package org.nypl.simplified.opds.auth_document.api

import java.io.InputStream
import java.net.URI

interface AuthenticationDocumentParsersType {

  fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean = false): AuthenticationDocumentParserType

}