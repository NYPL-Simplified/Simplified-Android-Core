package org.nypl.simplified.feeds.api

import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.feeds.api.Feed.FeedWithGroups
import org.nypl.simplified.feeds.api.Feed.FeedWithoutGroups
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Convenience functions for fetching and parsing feeds.
 */

object FeedLoading {

  /**
   * Synchronously load a URI, returning the first usable OPDS entry in the feed.
   *
   * @param feedLoader The feed loader that will be used
   * @param taskRecorder A task recorder used to record the operation steps
   * @param account The account that owns the feed
   * @param uri The target URI
   * @param timeout The timeout value
   * @param authenticate Whether the request should be authenticated
   * @param method The HTTP request method
   */

  fun loadSingleEntryFeed(
    feedLoader: FeedLoaderType,
    taskRecorder: TaskRecorderType,
    account: AccountReadableType,
    uri: URI,
    timeout: Pair<Long, TimeUnit>,
    authenticate: Boolean,
    method: String = "GET"
  ): FeedEntryOPDS {
    taskRecorder.beginNewStep("Fetching OPDS feed...")

    val feedResult =
      feedLoader.fetchURI(
        account = account,
        uri = uri,
        method = method,
        authenticate = authenticate,
      ).get(timeout.first, timeout.second)

    return when (feedResult) {
      is FeedLoaderFailedAuthentication -> {
        taskRecorder.currentStepFailed(feedResult.message, "feedAuthentication", feedResult.exception)
        throw feedResult.exception
      }
      is FeedLoaderFailedGeneral -> {
        taskRecorder.currentStepFailed(feedResult.message, "feedFailed", feedResult.exception)
        throw feedResult.exception
      }
      is FeedLoaderSuccess -> {
        taskRecorder.currentStepSucceeded("Feed retrieved and parsed.")
        taskRecorder.beginNewStep("Finding OPDS feed entry...")

        when (val feed = feedResult.feed) {
          is FeedWithGroups ->
            this.checkEntry(taskRecorder, findFirstInGroups(feed.feedGroupsInOrder))
          is FeedWithoutGroups ->
            this.checkEntry(taskRecorder, findFirst(feed.entriesInOrder))
        }
      }
    }
  }

  private fun checkEntry(
    taskRecorder: TaskRecorderType,
    feedOPDS: FeedEntryOPDS?
  ): FeedEntryOPDS =
    if (feedOPDS != null) {
      taskRecorder.currentStepSucceeded("Found OPDS feed entry.")
      feedOPDS
    } else {
      val message = "Expected a feed containing at least one OPDS entry"
      val exception = IllegalArgumentException(message)
      taskRecorder.currentStepFailed(message, "feedUnsuitable", exception)
      throw exception
    }

  private fun findFirst(
    feedEntries: List<FeedEntry>
  ): FeedEntryOPDS? {
    for (entry in feedEntries) {
      when (entry) {
        is FeedEntry.FeedEntryCorrupt ->
          Unit
        is FeedEntryOPDS ->
          return entry
      }
    }
    return null
  }

  private fun findFirstInGroups(
    feedGroupsInOrder: List<FeedGroup>
  ): FeedEntryOPDS? {
    for (group in feedGroupsInOrder) {
      for (entry in group.groupEntries) {
        when (entry) {
          is FeedEntry.FeedEntryCorrupt ->
            Unit
          is FeedEntryOPDS ->
            return entry
        }
      }
    }
    return null
  }
}
