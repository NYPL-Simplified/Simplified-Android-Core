package org.nypl.simplified.ui.catalog

import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import java.io.Serializable

/**
 * Parameters for a book detail page.
 */

data class CatalogFragmentBookDetailParameters(

  /**
   * The OPDS feed entry.
   */

  val feedEntry: FeedEntry.FeedEntryOPDS,

  /**
   * The parameters of the feed that lead to this book detail page.
   */

  val feedArguments: CatalogFeedArguments

) : Serializable {

  val bookID: BookID
    get() = this.feedEntry.bookID
}
