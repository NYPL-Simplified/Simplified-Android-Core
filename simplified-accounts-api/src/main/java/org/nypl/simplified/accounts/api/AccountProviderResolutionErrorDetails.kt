package org.nypl.simplified.accounts.api

import org.nypl.simplified.http.core.HTTPHasProblemReportType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.Presentables

/**
 * The details of a specific resolution error.
 *
 * @see [AccountProviderDescriptionType.resolve]
 */

sealed class AccountProviderResolutionErrorDetails : PresentableErrorType {

  /**
   * The ID of the account provider.
   */

  abstract val accountProviderID: String

  /**
   * The title (or "display name") of the account provider.
   */

  abstract val accountProviderTitle: String

  override val attributes: Map<String, String>
    get() = mapOf(
      Pair("Account ID", accountProviderID),
      Pair("Account", accountProviderTitle))

  /**
   * The authentication document link was unusable.
   */

  data class AuthDocumentUnusableLink(
    override val message: String,
    override val accountProviderID: String,
    override val accountProviderTitle: String
  ) : AccountProviderResolutionErrorDetails()

  /**
   * An HTTP request failed.
   */

  data class HTTPRequestFailed(
    override val message: String,
    val errorCode: Int,
    override val problemReport: HTTPProblemReport?,
    override val accountProviderID: String,
    override val accountProviderTitle: String
  ) : AccountProviderResolutionErrorDetails(), HTTPHasProblemReportType {

    override val attributes: Map<String, String>
      get() = Presentables.mergeProblemReportOptional(super.attributes.toMutableMap().apply {
        this["HTTP status code"] = errorCode.toString()
        this.toMap()
      }, this.problemReport)
  }

  /**
   * An unexpected exception occurred.
   */

  data class UnexpectedException(
    override val message: String,
    override val exception: Throwable,
    override val accountProviderID: String,
    override val accountProviderTitle: String
  ) : AccountProviderResolutionErrorDetails()

  /**
   * The authentication document was well-formed but was in some way unusable.
   */

  data class AuthDocumentUnusable(
    override val message: String,
    override val accountProviderID: String,
    override val accountProviderTitle: String
  ) : AccountProviderResolutionErrorDetails()

  /**
   * The authentication document was invalid.
   */

  data class AuthDocumentParseFailed(
    val warnings: List<ParseWarning>,
    val errors: List<ParseError>,
    override val message: String,
    override val accountProviderID: String,
    override val accountProviderTitle: String
  ) : AccountProviderResolutionErrorDetails()

  /**
   * No account source claimed to be able to resolve the description.
   */

  data class NoApplicableSource(
    override val message: String,
    override val accountProviderID: String,
    override val accountProviderTitle: String
  ) : AccountProviderResolutionErrorDetails()
}
