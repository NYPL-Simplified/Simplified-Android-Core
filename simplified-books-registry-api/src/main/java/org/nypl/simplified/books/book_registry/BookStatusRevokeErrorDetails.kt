package org.nypl.simplified.books.book_registry

import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import java.io.Serializable

/**
 * Data related to errors during book loan revocation.
 */

data class BookStatusRevokeErrorDetails(
  val problemReport: HTTPProblemReport? = null,
  val errorCode: String,
  override val message: String,
  override val attributes: Map<String, String> = mapOf(),
  override val exception: Throwable? = null
) : PresentableErrorType, Serializable
