package org.nypl.simplified.tests.local.opds.auth_document

import org.nypl.simplified.opds.auth_document.AuthenticationDocumentParsers
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.tests.opds.AuthenticationDocumentContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthenticationDocumentTest : AuthenticationDocumentContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AuthenticationDocumentTest::class.java)

  override val parsers: AuthenticationDocumentParsersType
    get() = AuthenticationDocumentParsers()

}
