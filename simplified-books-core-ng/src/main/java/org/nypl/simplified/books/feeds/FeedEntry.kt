package org.nypl.simplified.books.feeds

import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.Serializable

/**
 * The type of feed entries.
 */

sealed class FeedEntry : Serializable {

  abstract val bookID: BookID

  /**
   * A corrupt feed entry.
   *
   * This is used to represent entries that cannot be read from the database due
   * to corrupt metadata.
   */

  data class FeedEntryCorrupt(
    override val bookID: BookID,
    val error: Throwable)
    : FeedEntry()

  /**
   * An entry from an OPDS feed.
   */

  data class FeedEntryOPDS(
    val feedEntry: OPDSAcquisitionFeedEntry)
    : FeedEntry() {

    override val bookID: BookID
      get() = BookID.create(feedEntry.id)

    val probableFormat: BookFormats.BookFormatDefinition? =
      BookFormats.inferFormat(feedEntry)
  }

}
