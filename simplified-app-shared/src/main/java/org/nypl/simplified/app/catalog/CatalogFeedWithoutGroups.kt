package org.nypl.simplified.app.catalog

import android.database.DataSetObserver
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.common.base.Function
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Pair
import com.io7m.jfunctional.Unit
import com.io7m.jnull.NullCheck
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.Feed.FeedWithGroups
import org.nypl.simplified.feeds.api.Feed.FeedWithoutGroups
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

/**
 * A view that displays a catalog feed that does not contain any groups.
 */

class CatalogFeedWithoutGroups(
  private val activity: AppCompatActivity,
  private val analytics: AnalyticsType,
  private val account: AccountType,
  private val bookCoverProvider: BookCoverProviderType,
  private val bookSelectionListener: CatalogBookSelectionListenerType,
  private val bookRegistry: BookRegistryReadableType,
  private val bookController: BooksControllerType,
  private val profilesController: ProfilesControllerType,
  private val feedLoader: FeedLoaderType,
  private val feed: FeedWithoutGroups,
  private val networkConnectivity: NetworkConnectivityType,
  private val executor: ListeningExecutorService,
  private val screenSizeInformation: ScreenSizeInformationType
) : ListAdapter, OnScrollListener {

  private val adapter: ArrayAdapter<FeedEntry> =
    ArrayAdapter(this.activity, 0, this.feed.entriesInOrder)
  private val loading: AtomicReference<Pair<ListenableFuture<Unit>, URI>> =
    AtomicReference()
  private val uriNext: AtomicReference<URI> =
    AtomicReference<URI>(feed.feedNext)
  private val httpAuth: OptionType<HTTPAuthType> =
    createHttpAuth(this.account.loginState.credentials)

  private fun createHttpAuth(
    credentials: org.nypl.simplified.accounts.api.AccountAuthenticationCredentials?
  ): OptionType<HTTPAuthType> {
    return if (credentials != null) {
      Option.some(org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials))
    } else {
      Option.none<HTTPAuthType>()
    }
  }

  /**
   * Deliver a book status event to the view.
   *
   * @param event The event
   */

  fun onBookEvent(event: BookStatusEvent) {
    if (this.feed.containsBook(event.book())) {
      LOG.debug("update: updated feed entry")
      UIThread.runOnUIThread { this.adapter.notifyDataSetChanged() }
    }
  }

  override fun areAllItemsEnabled(): Boolean {
    return this.adapter.areAllItemsEnabled()
  }

  override fun getCount(): Int {
    return this.adapter.count
  }

  override fun getItem(position: Int): FeedEntry {
    return NullCheck.notNull<FeedEntry>(this.adapter.getItem(position)!!)
  }

  override fun getItemId(position: Int): Long {
    return this.adapter.getItemId(position)
  }

  override fun getItemViewType(position: Int): Int {
    return this.adapter.getItemViewType(position)
  }

  override fun getView(
    position: Int,
    @Nullable reused: View?,
    @Nullable parent: ViewGroup
  ): View {

    val e = NullCheck.notNull(this.adapter.getItem(position)!!)
    val cv: CatalogFeedBookCellView
    if (reused != null) {
      cv = reused as CatalogFeedBookCellView
    } else {
      cv = CatalogFeedBookCellView(
        activity = this.activity,
        analytics = this.analytics,
        coverProvider = this.bookCoverProvider,
        booksController = this.bookController,
        profilesController = this.profilesController,
        booksRegistry = this.bookRegistry,
        networkConnectivity = this.networkConnectivity,
        screenSizeInformation = this.screenSizeInformation)
    }

    cv.viewConfigure(e, this.bookSelectionListener)
    return cv
  }

  override fun getViewTypeCount(): Int {
    return this.adapter.viewTypeCount
  }

  override fun hasStableIds(): Boolean {
    return this.adapter.hasStableIds()
  }

  override fun isEmpty(): Boolean {
    return this.adapter.isEmpty
  }

  override fun isEnabled(position: Int): Boolean {
    return this.adapter.isEnabled(position)
  }

  /**
   * Attempt to load the next feed, if necessary. If the feed is already
   * loading, the feed will not be requested again.
   *
   * @param nextRef The next URI, if any
   * @return A future representing the loading feed
   */

  @Nullable
  private fun loadNext(nextRef: AtomicReference<URI>): Future<Unit>? {
    val next = nextRef.get()
    if (next != null) {
      val inLoading = this.loading.get()
      if (inLoading == null) {
        LOG.debug("no feed currently loading; loading next feed: {}", next)
        return this.loadNextActual(next)
      }

      val loadingUri = inLoading.right
      if (loadingUri != next) {
        LOG.debug("different feed currently loading; loading next feed: {}", next)
        return this.loadNextActual(next)
      }

      LOG.debug("already loading next feed, not loading again: {}", next)
    }

    return null
  }

  internal fun loadNextActual(next: URI): ListenableFuture<Unit> {
    LOG.debug("loading: {}", next)

    val future =
      this.feedLoader.fetchURIWithBookRegistryEntries(next, this.httpAuth)
        .catching(
          Exception::class.java,
          Function<Exception, FeedLoaderResult>(this@CatalogFeedWithoutGroups::wrapFeedLoaderException),
          this.executor)
        .transform(
          Function<FeedLoaderResult, Unit> { result -> this.onFeedResult(result!!) },
          this.executor)

    this.loading.set(Pair.pair<ListenableFuture<Unit>, URI>(future, next))
    return future
  }

  private fun wrapFeedLoaderException(ex: Exception?) =
    FeedLoaderFailedGeneral(null, ex!!, ex.localizedMessage, sortedMapOf())

  private fun onFeedResult(feedResult: FeedLoaderResult): Unit {
    return when (feedResult) {
      is FeedLoaderSuccess -> {
        onFeedSuccess(feedResult.feed)
      }
      is FeedLoaderFailedGeneral -> {
        if (feedResult.exception is CancellationException) {
          Unit.unit()
        } else {
          LOG.error("failed to load feed: ", feedResult.exception)
          Unit.unit()
        }
      }
      is FeedLoaderFailedAuthentication -> {
        throw UnimplementedCodeException()
      }
    }
  }

  private fun onFeedSuccess(feed: Feed): Unit {
    return when (feed) {
      is FeedWithoutGroups -> {
        LOG.debug("received feed without groups: {}", feed.feedID)

        this.feed.entriesInOrder.addAll(feed.entriesInOrder)
        this.uriNext.set(feed.feedNext)

        LOG.debug("current feed size: {}", this.feed.size)
        Unit.unit()
      }
      is FeedWithGroups -> {
        LOG.error("received feed with groups: {}", feed.feedID)
        Unit.unit()
      }
    }
  }

  override fun onScroll(
    @Nullable view: AbsListView,
    firstVisibleItem: Int,
    visibleCount: Int,
    totalCount: Int
  ) {

    /*
     * If the user is close enough to the end of the list, load the next feed.
     */

    if (shouldLoadNext(firstVisibleItem, totalCount)) {
      this.loadNext(this.uriNext)
    }
  }

  override fun onScrollStateChanged(
    @Nullable view: AbsListView,
    state: Int
  ) {
    when (state) {
      OnScrollListener.SCROLL_STATE_FLING, OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
        this.bookCoverProvider.loadingThumbailsPause()
      }
      OnScrollListener.SCROLL_STATE_IDLE -> {
        this.bookCoverProvider.loadingThumbnailsContinue()
      }
    }
  }

  override fun registerDataSetObserver(@Nullable observer: DataSetObserver) {
    this.adapter.registerDataSetObserver(observer)
  }

  override fun unregisterDataSetObserver(@Nullable observer: DataSetObserver) {
    this.adapter.unregisterDataSetObserver(observer)
  }

  companion object {

    private val LOG =
      LoggerFactory.getLogger(CatalogFeedWithoutGroups::class.java)

    private fun shouldLoadNext(
      firstVisibleItem: Int,
      totalCount: Int
    ): Boolean {

      LOG.debug("shouldLoadNext: {} - {} = {}",
        totalCount, firstVisibleItem, totalCount - firstVisibleItem)
      return totalCount - firstVisibleItem <= 50
    }
  }
}
