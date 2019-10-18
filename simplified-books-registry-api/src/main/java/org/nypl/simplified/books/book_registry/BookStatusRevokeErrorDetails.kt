package org.nypl.simplified.books.book_registry

import org.nypl.simplified.http.core.HTTPHasProblemReportType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.Presentables
import java.io.Serializable

/**
 * Data related to errors during book loan revocation.
 */

sealed class BookStatusRevokeErrorDetails : PresentableErrorType, Serializable {

  /**
   * The loan is not revocable.
   */

  data class NotRevocable(
    override val message: String)
    : BookStatusRevokeErrorDetails()

  /**
   * Credentials are required, but none are available.
   */

  data class NoCredentialsAvailable(
    override val message: String)
    : BookStatusRevokeErrorDetails()

  /**
   * Timed out waiting for an operation to complete.
   */

  data class TimedOut(
    override val message: String)
    : BookStatusRevokeErrorDetails()

  /**
   * An operation was cancelled.
   */

  data class Cancelled(
    override val message: String)
    : BookStatusRevokeErrorDetails()

  /**
   * Attempting to load the feed for a revoke URI failed.
   */

  data class FeedLoaderFailed(
    override val message: String,
    override val problemReport: HTTPProblemReport?,
    override val exception: Throwable?)
    : BookStatusRevokeErrorDetails(), HTTPHasProblemReportType {
    override val attributes: Map<String, String>
      get() = Presentables.mergeProblemReportOptional(super.attributes, this.problemReport)
  }

  /**
   * An OPDS feed contained a corrupted entry.
   */

  data class FeedCorrupted(
    override val exception: Throwable)
    : BookStatusRevokeErrorDetails() {
    override val message: String
      get() = this.exception.localizedMessage
  }

  /**
   * An OPDS feed was unusable for an unspecified reason.
   */

  data class FeedUnusable(
    override val message: String)
    : BookStatusRevokeErrorDetails()

  /**
   * Errors related to DRM.
   */

  sealed class DRMError : BookStatusRevokeErrorDetails() {

    /**
     * The name of the DRM system
     */

    abstract val system: String

    /**
     * A DRM system failed with an opaque error code.
     */

    data class DRMFailure(
      override val system: String,
      val errorCode: String,
      override val message: String)
      : DRMError()

    /**
     * A DRM system returned a content type that cannot be supported.
     */

    data class DRMUnsupportedContentType(
      override val system: String,
      val contentType: String,
      override val message: String)
      : DRMError()

    /**
     * A device is not active and therefore can't be used for DRM operations.
     */

    data class DRMDeviceNotActive(
      override val system: String,
      override val message: String)
      : DRMError()
  }

  /**
   * An unexpected exception occurred.
   */

  data class UnexpectedException(
    override val exception: Throwable)
    : BookStatusRevokeErrorDetails() {
    override val message: String
      get() = this.exception.localizedMessage
  }

}
