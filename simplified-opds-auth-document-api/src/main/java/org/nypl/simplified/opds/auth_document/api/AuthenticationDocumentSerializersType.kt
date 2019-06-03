package org.nypl.simplified.opds.auth_document.api

import java.io.OutputStream
import java.net.URI

interface AuthenticationDocumentSerializersType {

  fun createSerializer(
    uri: URI,
    stream: OutputStream,
    document: AuthenticationDocument): AuthenticationDocumentSerializerType

}