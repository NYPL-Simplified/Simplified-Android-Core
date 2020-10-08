package org.nypl.simplified.accounts.api

import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import java.io.Serializable

/**
 * An error value that indicates why creating an account failed.
 */

sealed class AccountCreateErrorDetails(
  val problemReport: HTTPProblemReport? = null,
  val errorCode: String,
  override val message: String,
  override val attributes: Map<String, String> = mapOf(),
  override val exception: Throwable? = null
) : PresentableErrorType, Serializable
