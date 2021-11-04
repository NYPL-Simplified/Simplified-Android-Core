package org.nypl.simplified.feeds.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.Serializable

/**
 * The type of feed entries.
 */

sealed class FeedEntry : Serializable {

  /**
   * The account that owns the feed entry
   */

  abstract val accountID: AccountID

  /**
   * The id of the book within the feed entry
   */

  abstract val bookID: BookID

  /**
   * A corrupt feed entry.
   *
   * This is used to represent entries that cannot be read from the database due
   * to corrupt metadata.
   */

  data class FeedEntryCorrupt(
    override val accountID: AccountID,
    override val bookID: BookID,
    val error: Throwable
  ) : FeedEntry()

  /**
   * An entry from an OPDS feed.
   */

  data class FeedEntryOPDS(
    override val accountID: AccountID,
    val feedEntry: OPDSAcquisitionFeedEntry
  ) : FeedEntry() {

    override val bookID: BookID =
      BookID.newFromOPDSAndAccount(feedEntry.id, accountID)

    val probableFormat: BookFormats.BookFormatDefinition? =
      BookFormats.inferFormat(feedEntry)
  }
}
