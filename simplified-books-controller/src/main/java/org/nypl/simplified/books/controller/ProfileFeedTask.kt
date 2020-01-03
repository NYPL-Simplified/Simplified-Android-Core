package org.nypl.simplified.books.controller

import com.io7m.jnull.NullCheck
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FacetType
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.Callable

internal class ProfileFeedTask(
  val bookRegistry: BookRegistryReadableType,
  val request: ProfileFeedRequest
) : Callable<Feed.FeedWithoutGroups> {

  override fun call(): Feed.FeedWithoutGroups {
    LOG.debug("generating local feed")

    /*
     * Generate facets.
     */

    val facetGroups = HashMap<String, List<FeedFacet>>(32)
    val facets = ArrayList<FeedFacet>(32)

    facets(this.request, facetGroups, facets)

    val feed =
      Feed.empty(
        feedURI = this.request.uri,
        feedID = this.request.id,
        feedSearch = FeedSearch.FeedSearchLocal,
        feedTitle = this.request.title)

    try {
      LOG.debug("book registry contains {} books", this.bookRegistry.books().size)
      val books = collectAllBooks(this.bookRegistry)
      LOG.debug("collected {} candidate books", books.size)

      val filter = selectFeedFilter(this.request)
      filterBooks(filter, books)
      LOG.debug("after filtering, {} candidate books remain", books.size)
      searchBooks(this.request.search, books)
      LOG.debug("after searching, {} candidate books remain", books.size)
      sortBooks(this.request.facetActive, books)
      LOG.debug("after sorting, {} candidate books remain", books.size)

      for (book in books) {
        feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(book.book.entry))
      }

      return feed
    } finally {
      LOG.debug("generated a local feed with {} entries", feed.size)
    }
  }

  /**
   * Filter the given books by the given search terms.
   */

  private fun searchBooks(
    search: String?,
    books: ArrayList<BookWithStatus>) {

    if (search == null) {
      return
    }

    val termsUpper = searchTermsSplitUpper(search)
    val iterator = books.iterator()
    while (iterator.hasNext()) {
      val book = iterator.next()
      if (!searchMatches(termsUpper, book)) {
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
      termsUpper.add(term.toUpperCase())
    }
    return termsUpper
  }

  /**
   * Sort the list of books by the given facet.
   */

  private fun sortBooks(
    facet: FacetType,
    books: ArrayList<BookWithStatus>) {

    when (facet) {
      FacetType.SORT_BY_AUTHOR -> sortBooksByAuthor(books)
      FacetType.SORT_BY_TITLE -> sortBooksByTitle(books)
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
        val author1 = NullCheck.notNull(authors1[0])
        val author2 = NullCheck.notNull(authors2[0])
        author1.compareTo(author2)
      }
    }
  }

  /**
   * Filter the list of books with the given filter.
   */

  private fun filterBooks(
    filter: (BookStatus) -> Boolean,
    books: ArrayList<BookWithStatus>) {

    val iter = books.iterator()
    while (iter.hasNext()) {
      val book = iter.next()
      if (!filter.invoke(book.status)) {
        iter.remove()
      }
    }
  }

  private fun collectAllBooks(book_registry: BookRegistryReadableType): ArrayList<BookWithStatus> {
    val accountID = this.request.filterByAccountID
    val values = book_registry.books().values
    return ArrayList(if (accountID != null) {
      values.filter { book -> book.book.account == accountID }
    } else {
      values
    })
  }


  companion object {

    private val LOG = LoggerFactory.getLogger(ProfileFeedTask::class.java)

    private fun usableForBooksFeed(status: BookStatus): Boolean {
      return when (status) {
        is BookStatus.Held,
        is BookStatus.Holdable,
        is BookStatus.Loanable,
        is BookStatus.Revoked ->
          false

        is BookStatus.Downloading,
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
        is BookStatus.Held,
        is BookStatus.Holdable ->
          true

        is BookStatus.Downloading,
        is BookStatus.FailedDownload,
        is BookStatus.FailedLoan,
        is BookStatus.FailedRevoke,
        is BookStatus.Loanable,
        is BookStatus.Loaned,
        is BookStatus.RequestingDownload,
        is BookStatus.RequestingLoan,
        is BookStatus.RequestingRevoke,
        is BookStatus.Revoked ->
          false
      }
    }

    private fun facets(
      request: ProfileFeedRequest,
      facet_groups: HashMap<String, List<FeedFacet>>,
      facets: ArrayList<FeedFacet>) {
      val values = FacetType.values()
      for (v in values) {
        val active = v == request.facetActive
        val f = FeedFacet.FeedFacetPseudo(request.facetTitleProvider.getTitle(v), active, v)
        facets.add(f)
      }
      facet_groups[request.facetGroup] = facets
    }

    /**
     * @return `true` if any of the given search terms match the given book, or the list of
     * search terms is empty
     */

    private fun searchMatches(
      terms_upper: List<String>,
      book: BookWithStatus): Boolean {

      if (terms_upper.isEmpty()) {
        return true
      }

      for (index in terms_upper.indices) {
        val term_upper = terms_upper[index]
        val ee = book.book.entry
        val e_title = ee.title.toUpperCase()
        if (e_title.contains(term_upper)) {
          return true
        }

        val authors = ee.authors
        for (a in authors) {
          if (a.toUpperCase().contains(term_upper)) {
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
}
