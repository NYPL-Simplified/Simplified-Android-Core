package org.nypl.simplified.opds.auth_document.api

sealed class AuthenticationDocumentParseResult<T> {

  data class Success<T>(
    val warnings: List<AuthenticationDocumentWarning>,
    val result: T)
    : AuthenticationDocumentParseResult<T>()

  data class Failure<T>(
    val warnings: List<AuthenticationDocumentWarning>,
    val errors: List<AuthenticationDocumentError>)
    : AuthenticationDocumentParseResult<T>()
}
