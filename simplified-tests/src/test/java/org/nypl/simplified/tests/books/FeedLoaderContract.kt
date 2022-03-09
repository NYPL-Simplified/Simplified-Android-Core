package org.nypl.simplified.tests.books

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.tests.mocking.MockAccount
import java.net.URI
import java.util.concurrent.Executors

abstract class FeedLoaderContract {

  abstract fun createFeedLoader(exec: ListeningExecutorService): FeedLoaderType

  abstract fun resource(name: String): URI

  private lateinit var exec: ListeningExecutorService
  private lateinit var account: AccountReadableType

  @BeforeEach
  fun setup() {
    this.exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))
    this.account = MockAccount(AccountID.generate())
  }

  @AfterEach
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
        account = this.account,
        uri = resource("feed-no-usable-acquisitions.xml"),
        method = "GET"
      )
    val result =
      future.get()

    Assertions.assertTrue(result is FeedLoaderResult.FeedLoaderSuccess)
    val feed = (result as FeedLoaderResult.FeedLoaderSuccess).feed
    Assertions.assertEquals(0, feed.size)
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
        account = this.account,
        uri = resource("feed-only-buy-acquisitions.xml"),
        method = "GET"
      )
    val result =
      future.get()

    Assertions.assertTrue(result is FeedLoaderResult.FeedLoaderSuccess)
    val feed = (result as FeedLoaderResult.FeedLoaderSuccess).feed
    Assertions.assertEquals(0, feed.size)
  }
}
