package org.nypl.simplified.feeds.api

import org.nypl.simplified.http.core.HTTPHasProblemReportType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.Presentables
import java.net.URI

/**
 * The result of loading a feed.
 */

sealed class FeedLoaderResult {

  /**
   * The feed was loaded successfully.
   */

  data class FeedLoaderSuccess(
    val feed: Feed
  ) : FeedLoaderResult()

  /**
   * The feed failed to load.
   */

  sealed class FeedLoaderFailure : FeedLoaderResult(), HTTPHasProblemReportType, PresentableErrorType {

    /**
     * The feed failed to load due to the given exception.
     */

    data class FeedLoaderFailedGeneral(
      override val problemReport: HTTPProblemReport?,
      override val exception: Exception,
      override val message: String,
      private val attributesInitial: Map<String, String>
    ) : FeedLoaderFailure() {
      override val attributes: Map<String, String>
        get() = Presentables.mergeProblemReportOptional(this.attributesInitial, this.problemReport)
    }

    /**
     * The feed failed to load due to an authentication error.
     */

    data class FeedLoaderFailedAuthentication(
      override val problemReport: HTTPProblemReport?,
      override val exception: Exception,
      override val message: String,
      private val attributesInitial: Map<String, String>
    ) : FeedLoaderFailure() {
      override val attributes: Map<String, String>
        get() = Presentables.mergeProblemReportOptional(this.attributesInitial, this.problemReport)
    }
  }

  companion object {

    /**
     * Wrap an exception, producing a general feed loading error.
     */

    fun wrapException(
      uri: URI,
      exception: Throwable
    ): FeedLoaderFailure {
      return FeedLoaderFailure.FeedLoaderFailedGeneral(
        problemReport = null,
        exception =
          if (exception is java.lang.Exception) exception
          else java.lang.Exception(exception),
        message = exception.localizedMessage ?: "",
        attributesInitial = sortedMapOf(Pair("Feed URI", uri.toASCIIString()))
      )
    }
  }
}
