package org.nypl.simplified.ui.catalog

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import java.io.Serializable

/**
 * Parameters for a book detail page.
 */

data class CatalogFragmentBookDetailParameters(

  /**
   * The account to which the OPDS feed entry belongs.
   */

  val accountId: AccountID,

  /**
   * The OPDS feed entry.
   */

  val feedEntry: FeedEntry.FeedEntryOPDS

) : Serializable {

  val bookID: BookID
    get() = this.feedEntry.bookID

}
