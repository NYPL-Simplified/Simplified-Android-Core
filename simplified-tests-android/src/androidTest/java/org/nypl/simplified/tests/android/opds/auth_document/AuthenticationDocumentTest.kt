package org.nypl.simplified.tests.android.opds.auth_document

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.simplified.opds.auth_document.AuthenticationDocumentParsers
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.tests.opds.AuthenticationDocumentContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@SmallTest
class AuthenticationDocumentTest : AuthenticationDocumentContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AuthenticationDocumentTest::class.java)

  override val parsers: AuthenticationDocumentParsersType
    get() = AuthenticationDocumentParsers()

}
