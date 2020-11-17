package org.nypl.simplified.feeds.api

import org.librarysimplified.http.api.LSHTTPProblemReport
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

  sealed class FeedLoaderFailure : FeedLoaderResult(), PresentableErrorType {

    /**
     * The feed failed to load due to the given exception.
     */

    data class FeedLoaderFailedGeneral(
      val problemReport: LSHTTPProblemReport?,
      override val exception: Exception,
      override val message: String,
      private val attributesInitial: Map<String, String>
    ) : FeedLoaderFailure() {
      override val attributes: Map<String, String>
        get() = Presentables.mergeAttributes(
          map0 = this.attributesInitial,
          map1 = this.problemReport?.toMap() ?: emptyMap()
        )
    }

    /**
     * The feed failed to load due to an authentication error.
     */

    data class FeedLoaderFailedAuthentication(
      val problemReport: LSHTTPProblemReport?,
      override val exception: Exception,
      override val message: String,
      private val attributesInitial: Map<String, String>
    ) : FeedLoaderFailure() {
      override val attributes: Map<String, String>
        get() = Presentables.mergeAttributes(
          map0 = this.attributesInitial,
          map1 = this.problemReport?.toMap() ?: emptyMap()
        )
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
