package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition

/**
 * An entry from an OPDS feed.
 */

data class FeedEntryOPDS private constructor(
  val actualBookID: BookID,
  val feedEntry: OPDSAcquisitionFeedEntry,
  val probableFormat: OptionType<BookFormatDefinition>) : FeedEntryType {

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

    fun fromOPDSAcquisitionFeedEntry(entry: OPDSAcquisitionFeedEntry): FeedEntryType {
      return FeedEntryOPDS(BookID.newIDFromEntry(entry), entry, BookFormats.inferFormat(entry))
    }
  }
}
