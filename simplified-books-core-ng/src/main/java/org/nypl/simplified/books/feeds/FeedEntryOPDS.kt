package org.nypl.simplified.books.feeds

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_database.BookIDs

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

/**
 * An entry from an OPDS feed.
 */

data class FeedEntryOPDS private constructor(
  val actualBookID: BookID,
  val feedEntry: OPDSAcquisitionFeedEntry,
  val probableFormat: OptionType<BookFormats.BookFormatDefinition>) : FeedEntryType {

  override fun getBookID(): BookID {
    return this.actualBookID
  }

  override fun <A, E : Exception> matchFeedEntry(
    m: FeedEntryMatcherType<A, E>): A {
    return m.onFeedEntryOPDS(this)
  }

  companion object {

    private const val serialVersionUID = 1L

    /**
     * Construct a feed entry from the given OPDS feed entry.
     *
     * @param entry The entry
     * @return A feed entry
     */

    fun fromOPDSAcquisitionFeedEntry(entry: OPDSAcquisitionFeedEntry): FeedEntryOPDS {
      return FeedEntryOPDS(BookIDs.newFromOPDSEntry(entry), entry, BookFormats.inferFormat(entry))
    }
  }
}
