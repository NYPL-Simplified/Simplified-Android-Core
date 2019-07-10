package org.nypl.simplified.accounts.api

import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseWarning

/**
 * The details of a specific resolution error.
 *
 * @see [AccountProviderDescriptionType.resolve]
 */

sealed class AccountProviderResolutionErrorDetails {

  data class HTTPRequestFailed(
    val message: String,
    val errorCode: Int,
    val problemReport: HTTPProblemReport?)
    : AccountProviderResolutionErrorDetails()

  data class AuthDocumentParseFailed(
    val warnings: List<ParseWarning>,
    val errors: List<ParseError>)
    : AccountProviderResolutionErrorDetails()

}
