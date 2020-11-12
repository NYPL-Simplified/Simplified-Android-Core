package org.nypl.simplified.tests.books

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors

abstract class FeedLoaderContract {

  abstract fun createFeedLoader(exec: ListeningExecutorService): FeedLoaderType

  abstract fun resource(name: String): URI

  private lateinit var exec: ListeningExecutorService

  @Before
  fun setup() {
    this.exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))
  }

  @After
  fun tearDown() {
    this.exec.shutdown()
  }

  /**
   * An entry with no usable acquisitions should not appear in a feed at all.
   */

  @Test
  fun testFeedWithZeroAcquisitions() {
    val loader =
      this.createFeedLoader(this.exec)
    val future =
      loader.fetchURI(
        AccountID(UUID.randomUUID()),
        resource("feed-no-usable-acquisitions.xml"),
        null
      )
    val result =
      future.get()

    Assert.assertTrue(result is FeedLoaderResult.FeedLoaderSuccess)
    val feed = (result as FeedLoaderResult.FeedLoaderSuccess).feed
    Assert.assertEquals(0, feed.size)
  }

  /**
   * An entry with no usable acquisitions should not appear in a feed at all.
   */

  @Test
  fun testFeedWithOnlyBuyAcquisitions() {
    val loader =
      this.createFeedLoader(this.exec)
    val future =
      loader.fetchURI(
        AccountID(UUID.randomUUID()),
        resource("feed-only-buy-acquisitions.xml"),
        null
      )
    val result =
      future.get()

    Assert.assertTrue(result is FeedLoaderResult.FeedLoaderSuccess)
    val feed = (result as FeedLoaderResult.FeedLoaderSuccess).feed
    Assert.assertEquals(0, feed.size)
  }
}
