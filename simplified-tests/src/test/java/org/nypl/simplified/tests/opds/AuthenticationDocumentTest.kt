package org.nypl.simplified.tests.opds

import org.nypl.simplified.opds.auth_document.AuthenticationDocumentParsers
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthenticationDocumentTest : AuthenticationDocumentContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AuthenticationDocumentTest::class.java)

  override val parsers: AuthenticationDocumentParsersType
    get() = AuthenticationDocumentParsers()
}
