package org.nypl.simplified.books.book_registry

import org.nypl.simplified.http.core.HTTPProblemReport

/**
 * Data related to errors during book loan revocation.
 */

sealed class BookStatusRevokeErrorDetails {

  /**
   * The loan is not revocable.
   */

  object NotRevocable : BookStatusRevokeErrorDetails()

  /**
   * Credentials are required, but none are available.
   */

  object NoCredentialsAvailable : BookStatusRevokeErrorDetails()

  /**
   * Attempting to load the feed for a revoke URI failed.
   */

  data class FeedLoaderFailed(
    val errorReport: HTTPProblemReport?,
    val exception: Throwable?)
    : BookStatusRevokeErrorDetails()

  /**
   * An OPDS feed contained a corrupted entry.
   */

  data class FeedCorrupted(
    val exception: Throwable)
    : BookStatusRevokeErrorDetails()

  /**
   * An OPDS feed was unusable for an unspecified reason.
   */

  object FeedUnusable
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
     * A specific DRM system is not supported.
     */

    data class DRMUnsupportedSystem(
      override val system: String)
      : DRMError()

    /**
     * A DRM system failed with an opaque error code.
     */

    data class DRMFailure(
      override val system: String,
      val errorCode: String)
      : DRMError()

    /**
     * A DRM system returned a content type that cannot be supported.
     */

    data class DRMUnsupportedContentType(
      override val system: String,
      val contentType: String)
      : DRMError()

    /**
     * A device is not active and therefore can't be used for DRM operations.
     */

    data class DRMDeviceNotActive(
      override val system: String)
      : DRMError()

    /**
     * An ACSM file was unparseable.
     */

    data class DRMUnparseableACSM(
      override val system: String)
      : DRMError()

    /**
     * An ACSM file was unreadable.
     */

    data class DRMUnreadableACSM(
      override val system: String)
      : DRMError()
  }


}
