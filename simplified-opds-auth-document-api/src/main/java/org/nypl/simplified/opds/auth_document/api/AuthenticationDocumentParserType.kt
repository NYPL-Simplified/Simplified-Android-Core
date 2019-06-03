package org.nypl.simplified.opds.auth_document.api

import java.io.Closeable

interface AuthenticationDocumentParserType : Closeable {

  fun parse(): AuthenticationDocumentParseResult<AuthenticationDocument>

}