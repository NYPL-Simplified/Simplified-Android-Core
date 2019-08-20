package org.nypl.simplified.accounts.api

import com.google.common.base.Preconditions
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import java.net.URI

/**
 * An error value that indicates why creating an account failed.
 */

sealed class AccountCreateErrorDetails : PresentableErrorType {

  /**
   * An account could not be created because an account provider could not be resolved.
   */

  data class AccountProviderResolutionFailed(
    val errorValues: List<AccountProviderResolutionErrorDetails>)
    : AccountCreateErrorDetails() {

    init {
      Preconditions.checkArgument(
        this.errorValues.isNotEmpty(),
        "Must have logged at least one error")
    }

    override val message: String
      get() = this.errorValues[0].message
    override val causes: List<PresentableErrorType>
      get() = this.errorValues
  }

  /**
   * An unexpected exception occurred.
   */

  data class UnexpectedException(
    override val message: String,
    override val exception: Throwable)
    : AccountCreateErrorDetails()

  /**
   * An HTTP request could not be made.
   */

  data class HTTPRequestFailed(
    override val message: String,
    val opdsURI: URI,
    val status: Int,
    val problemReport: OptionType<HTTPProblemReport>)
    : AccountCreateErrorDetails()

}
