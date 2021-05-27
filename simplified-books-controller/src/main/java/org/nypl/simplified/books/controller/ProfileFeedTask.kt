package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FilteringForAccount
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting.SortBy
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Callable

internal class ProfileFeedTask(
  val bookRegistry: BookRegistryReadableType,
  val profiles: ProfilesControllerType,
  val request: ProfileFeedRequest
) : Callable<Feed.FeedWithoutGroups> {

  private val logger =
    LoggerFactory.getLogger(ProfileFeedTask::class.java)

  override fun call(): Feed.FeedWithoutGroups {
    this.logger.debug("generating local feed")

    /*
     * Generate facets.
     */

    val facetGroups = this.makeFacets()
    val facets = facetGroups.values.flatten()

    val feed =
      Feed.empty(
        feedURI = this.request.uri,
        feedID = this.request.id,
        feedSearch = FeedSearch.FeedSearchLocal,
        feedTitle = this.request.title,
        feedFacets = facets,
        feedFacetGroups = facetGroups
      )

    try {
      this.logger.debug("book registry contains {} books", this.bookRegistry.books().size)
      val books = this.collectAllBooks(this.bookRegistry)
      this.logger.debug("collected {} candidate books", books.size)

      val filter = this.selectFeedFilter(this.request)
      this.filterBooks(filter, books)
      this.logger.debug("after filtering, {} candidate books remain", books.size)
      this.searchBooks(this.request.search, books)
      this.logger.debug("after searching, {} candidate books remain", books.size)
      this.sortBooks(this.request.sortBy, books)
      this.logger.debug("after sorting, {} candidate books remain", books.size)

      for (book in books) {
        feed.entriesInOrder.add(
          FeedEntry.FeedEntryOPDS(
            accountID = book.book.account,
            feedEntry = book.book.entry
          )
        )
      }

      return feed
    } finally {
      this.logger.debug("generated a local feed with {} entries", feed.size)
    }
  }

  private fun makeFacets(): Map<String, List<FeedFacet>> {
    val sorting = this.makeSortingFacets()
    val filtering = this.makeFilteringFacets()
    val results = mutableMapOf<String, List<FeedFacet>>()
    results[sorting.first] = sorting.second
    results[filtering.first] = filtering.second
    check(results.size == 2)
    return results.toMap()
  }

  private fun makeFilteringFacets(): Pair<String, List<FeedFacet>> {
    val facets = mutableListOf<FeedFacet>()
    val accounts = this.profiles.profileCurrent().accounts().values
    for (account in accounts) {
      val active = account.id == this.request.filterByAccountID
      val title = account.provider.displayName
      facets.add(FilteringForAccount(title, active, account.id))
    }

    facets.add(
      FilteringForAccount(
        title = this.request.facetTitleProvider.collectionAll,
        isActive = this.request.filterByAccountID == null,
        account = null
      )
    )
    return Pair(this.request.facetTitleProvider.collection, facets)
  }

  private fun makeSortingFacets(): Pair<String, List<FeedFacet>> {
    val facets = mutableListOf<FeedFacet>()
    val values = SortBy.values()
    for (sortingFacet in values) {
      val active = sortingFacet == this.request.sortBy
      val title =
        when (sortingFacet) {
          SortBy.SORT_BY_AUTHOR -> this.request.facetTitleProvider.sortByAuthor
          SortBy.SORT_BY_TITLE -> this.request.facetTitleProvider.sortByTitle
        }
      facets.add(Sorting(title, active, sortingFacet))
    }
    return Pair(this.request.facetTitleProvider.sortBy, facets)
  }

  /**
   * Filter the given books by the given search terms.
   */

  private fun searchBooks(
    search: String?,
    books: ArrayList<BookWithStatus>
  ) {
    if (search == null) {
      return
    }

    val termsUpper = this.searchTermsSplitUpper(search)
    val iterator = books.iterator()
    while (iterator.hasNext()) {
      val book = iterator.next()
      if (!this.searchMatches(termsUpper, book)) {
        iterator.remove()
      }
    }
  }

  /**
   * Split the given search string into a list of uppercase search terms.
   */

  private fun searchTermsSplitUpper(search: String): List<String> {
    val terms = search.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val termsUpper = ArrayList<String>(8)
    for (term in terms) {
      termsUpper.add(term.toUpperCase(Locale.ROOT))
    }
    return termsUpper
  }

  /**
   * Sort the list of books by the given facet.
   */

  private fun sortBooks(
    sortBy: SortBy,
    books: ArrayList<BookWithStatus>
  ) {
    when (sortBy) {
      SortBy.SORT_BY_AUTHOR -> this.sortBooksByAuthor(books)
      SortBy.SORT_BY_TITLE -> this.sortBooksByTitle(books)
    }
  }

  private fun sortBooksByTitle(books: ArrayList<BookWithStatus>) {
    Collections.sort(books) { book0, book1 ->
      val entry0 = book0.book.entry
      val entry1 = book1.book.entry
      entry0.title.compareTo(entry1.title)
    }
  }

  private fun sortBooksByAuthor(books: ArrayList<BookWithStatus>) {
    Collections.sort(books) { book0, book1 ->
      val entry0 = book0.book.entry
      val entry1 = book1.book.entry
      val authors1 = entry0.authors
      val authors2 = entry1.authors
      val e0 = authors1.isEmpty()
      val e1 = authors2.isEmpty()
      if (e0 && e1) {
        0
      } else if (e0) {
        1
      } else if (e1) {
        -1
      } else {
        val author1 = authors1[0]!!
        val author2 = authors2[0]!!
        author1.compareTo(author2)
      }
    }
  }

  /**
   * Filter the list of books with the given filter.
   */

  private fun filterBooks(
    filter: (BookStatus) -> Boolean,
    books: ArrayList<BookWithStatus>
  ) {
    val iter = books.iterator()
    while (iter.hasNext()) {
      val book = iter.next()
      if (!filter.invoke(book.status)) {
        iter.remove()
      }
    }
  }

  private fun collectAllBooks(bookRegistry: BookRegistryReadableType): ArrayList<BookWithStatus> {
    val accountID = this.request.filterByAccountID
    val values = bookRegistry.books().values
    val allBooks =
      if (accountID != null) {
        values.filter { book -> book.book.account == accountID }
      } else {
        values
      }.filter {
        this.accountIsLoggedIn(it.book.account)
      }
    return ArrayList(allBooks)
  }

  private fun accountIsLoggedIn(accountID: AccountID): Boolean {
    return try {
      val account = this.profiles.profileCurrent().account(accountID)
      if (!account.provider.authentication.isLoginPossible) {
        true
      } else {
        account.loginState is AccountLoginState.AccountLoggedIn
      }
    } catch (e: Exception) {
      false
    }
  }

  private fun usableForBooksFeed(status: BookStatus): Boolean {
    return when (status) {
      is BookStatus.Held,
      is BookStatus.Holdable,
      is BookStatus.Loanable,
      is BookStatus.Revoked ->
        false

      is BookStatus.Downloading,
      is BookStatus.DownloadWaitingForExternalAuthentication,
      is BookStatus.DownloadExternalAuthenticationInProgress,
      is BookStatus.FailedDownload,
      is BookStatus.FailedLoan,
      is BookStatus.FailedRevoke,
      is BookStatus.Loaned,
      is BookStatus.RequestingDownload,
      is BookStatus.RequestingLoan,
      is BookStatus.RequestingRevoke ->
        true
    }
  }

  private fun usableForHoldsFeed(status: BookStatus): Boolean {
    return when (status) {
      is BookStatus.Held ->
        true

      is BookStatus.Downloading,
      is BookStatus.DownloadWaitingForExternalAuthentication,
      is BookStatus.DownloadExternalAuthenticationInProgress,
      is BookStatus.FailedDownload,
      is BookStatus.FailedLoan,
      is BookStatus.FailedRevoke,
      is BookStatus.Holdable,
      is BookStatus.Loanable,
      is BookStatus.Loaned,
      is BookStatus.RequestingDownload,
      is BookStatus.RequestingLoan,
      is BookStatus.RequestingRevoke,
      is BookStatus.Revoked ->
        false
    }
  }

  /**
   * @return `true` if any of the given search terms match the given book, or the list of
   * search terms is empty
   */

  private fun searchMatches(
    termsUpper: List<String>,
    book: BookWithStatus
  ): Boolean {
    if (termsUpper.isEmpty()) {
      return true
    }

    for (index in termsUpper.indices) {
      val termUpper = termsUpper[index]
      val ee = book.book.entry
      val eTitle = ee.title.toUpperCase(Locale.ROOT)
      if (eTitle.contains(termUpper)) {
        return true
      }

      val authors = ee.authors
      for (a in authors) {
        if (a.toUpperCase(Locale.ROOT).contains(termUpper)) {
          return true
        }
      }
    }

    return false
  }

  private fun selectFeedFilter(
    request: ProfileFeedRequest
  ): (BookStatus) -> Boolean {
    return when (request.feedSelection) {
      FeedBooksSelection.BOOKS_FEED_LOANED -> ::usableForBooksFeed
      FeedBooksSelection.BOOKS_FEED_HOLDS -> ::usableForHoldsFeed
    }
  }
}
