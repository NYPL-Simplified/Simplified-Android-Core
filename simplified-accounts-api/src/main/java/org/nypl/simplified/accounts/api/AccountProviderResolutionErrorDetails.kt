package org.nypl.simplified.accounts.api

import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.presentableerror.api.PresentableErrorType

/**
 * The details of a specific resolution error.
 *
 * @see [AccountProviderDescriptionType.resolve]
 */

sealed class AccountProviderResolutionErrorDetails : PresentableErrorType {

  /**
   * The authentication document link was unusable.
   */

  data class AuthDocumentUnusableLink(
    override val message: String)
    : AccountProviderResolutionErrorDetails()

  /**
   * An HTTP request failed.
   */

  data class HTTPRequestFailed(
    override val message: String,
    val errorCode: Int,
    val problemReport: HTTPProblemReport?)
    : AccountProviderResolutionErrorDetails() {

    override val attributes: Map<String, String>

    init {
      val attrs = mutableMapOf<String, String>()
      attrs["errorCode"] = this.errorCode.toString()
      this.problemReport?.let { report ->
        attrs["problemReport.detail"] = report.problemDetail
        attrs["problemReport.status"] = report.problemStatus.toString()
        attrs["problemReport.title"] = report.problemTitle
        attrs["problemReport.type"] = report.problemType.toString()
      }
      this.attributes = attrs.toMap()
    }
  }

  /**
   * An unexpected exception occurred.
   */

  data class UnexpectedException(
    override val message: String,
    override val exception: Throwable)
    : AccountProviderResolutionErrorDetails()

  /**
   * The authentication document was well-formed but was in some way unusable.
   */

  data class AuthDocumentUnusable(
    override val message: String)
    : AccountProviderResolutionErrorDetails()

  /**
   * The authentication document was invalid.
   */

  data class AuthDocumentParseFailed(
    val warnings: List<ParseWarning>,
    val errors: List<ParseError>,
    override val message: String)
    : AccountProviderResolutionErrorDetails()

}
