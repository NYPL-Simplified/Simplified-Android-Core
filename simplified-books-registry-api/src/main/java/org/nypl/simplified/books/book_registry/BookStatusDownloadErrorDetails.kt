package org.nypl.simplified.books.book_registry

import org.nypl.simplified.http.core.HTTPHasProblemReportType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.Presentables
import java.io.Serializable

/**
 * Data related to errors during book borrowing.
 */

sealed class BookStatusDownloadErrorDetails : PresentableErrorType, Serializable {

  /**
   * An HTTP request failed.
   */

  data class HTTPRequestFailed(
    val status: Int,
    override val problemReport: HTTPProblemReport?,
    override val message: String,
    val attributesInitial: Map<String, String>
  ) : BookStatusDownloadErrorDetails(), HTTPHasProblemReportType {
    override val attributes: Map<String, String>
      get() = Presentables.mergeProblemReportOptional(this.attributesInitial, this.problemReport)
  }

  /**
   * Attempting to load the feed for a borrow URI failed.
   */

  data class FeedLoaderFailed(
    override val message: String,
    override val problemReport: HTTPProblemReport?,
    override val exception: Throwable?,
    val attributesInitial: Map<String, String>
  ) : BookStatusDownloadErrorDetails(), HTTPHasProblemReportType {
    override val attributes: Map<String, String>
      get() = Presentables.mergeProblemReportOptional(this.attributesInitial, this.problemReport)
  }

  /**
   * An OPDS feed contained a corrupted entry.
   */

  data class FeedCorrupted(
    override val exception: Throwable,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails() {
    override val message: String
      get() = this.exception.localizedMessage
  }

  /**
   * An OPDS feed was unusable for an unspecified reason.
   */

  data class FeedUnusable(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  /**
   * A problem occurred with the book database.
   */

  data class BookDatabaseFailed(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  /**
   * An acquisition relation is not supported.
   */

  data class UnsupportedAcquisition(
    override val message: String,
    val type: OPDSAcquisition.Relation,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  /**
   * Errors related to DRM.
   */

  sealed class DRMError : BookStatusDownloadErrorDetails() {

    /**
     * The name of the DRM system
     */

    abstract val system: String

    /**
     * A specific DRM system is not supported.
     */

    data class DRMUnsupportedSystem(
      override val system: String,
      override val message: String
    ) : DRMError()

    /**
     * A DRM system failed with an opaque error code.
     */

    data class DRMFailure(
      override val system: String,
      val errorCode: String,
      override val message: String
    ) : DRMError()

    /**
     * A DRM system returned a content type that cannot be supported.
     */

    data class DRMUnsupportedContentType(
      override val system: String,
      val contentType: String,
      override val message: String
    ) : DRMError()

    /**
     * A device is not active and therefore can't be used for DRM operations.
     */

    data class DRMDeviceNotActive(
      override val system: String,
      override val message: String
    ) : DRMError()

    /**
     * An ACSM file was unparseable.
     */

    data class DRMUnparseableACSM(
      override val system: String,
      override val message: String
    ) : DRMError()

    /**
     * An ACSM file was unreadable.
     */

    data class DRMUnreadableACSM(
      override val system: String,
      override val message: String
    ) : DRMError()
  }

  data class UnusableAcquisitions(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class WrongAvailability(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class TimedOut(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class UnsupportedType(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class UnparseableBearerToken(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class BundledCopyFailed(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class ContentCopyFailed(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class DownloadCancelled(
    override val message: String,
    override val attributes: Map<String, String>
  ) : BookStatusDownloadErrorDetails()

  data class UnexpectedException(
    override val exception: Throwable,
    override val attributes: Map<String, String> = mapOf()
  ) : BookStatusDownloadErrorDetails() {
    override val message: String
      get() = this.exception.localizedMessage
  }
}
