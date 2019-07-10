package org.nypl.simplified.feeds.api

import org.nypl.simplified.http.core.HTTPProblemReport
import java.lang.Exception

/**
 * The result of loading a feed.
 */

sealed class FeedLoaderResult {

  /**
   * The feed was loaded successfully.
   */

  data class FeedLoaderSuccess(
    val feed: Feed)
    : FeedLoaderResult()

  /**
   * The feed failed to load.
   */

  sealed class FeedLoaderFailure : FeedLoaderResult() {

    /**
     * The feed failed to load due to the given exception.
     */

    data class FeedLoaderFailedGeneral(
      val problemReport: HTTPProblemReport?,
      val exception: Exception)
      : FeedLoaderFailure()

    /**
     * The feed failed to load due to an authentication error.
     */

    data class FeedLoaderFailedAuthentication(
      val problemReport: HTTPProblemReport?,
      val exception: Exception)
      : FeedLoaderFailure()

  }

}